# -*- encoding: utf-8 -*-
import glob
import csv
import networkx as nx
import bottle
from geojson import Feature, FeatureCollection, dumps
from bottle import view, route, response, request, run, static_file, HTTPResponse
import os

# for Apache - http://bottlepy.org/docs/dev/faq.html
# “TEMPLATE NOT FOUND” IN MOD_WSGI/MOD_PYTHON
bottle.TEMPLATE_PATH.insert(0, os.path.join(os.path.dirname(os.path.realpath(__file__)), 'views'))


# Инициализация графа
def init_graph(city):

    graph = nx.Graph()
    nodes = csv.DictReader(open(os.path.join(os.path.dirname(__file__), '../data/%s/stations.csv' % city), 'rb'),delimiter=';')
    edges = csv.DictReader(open(os.path.join(os.path.dirname(__file__), '../data/%s/graph.csv' % city), 'rb'), delimiter=';')

    for node in nodes:
        graph.add_node(int(node['id_station']))

    for edge in edges:
        graph.add_edge(
            int(edge['id_from']),
            int(edge['id_to']),
            weight=int(edge['cost'])
        )

    return graph


# Извлечение следующего элемента в списке
def get_next_item(array, item):
    item_index = array.index(item)
    try:
        next_item = array[item_index + 1]
    except:
        next_item = array[item_index]
    return next_item


# Извлечение информации о препятствиях
def get_barriers(item):
    return dict(
        max_width=int(item['max_width'])/10 if item['max_width'].isdigit() else item['max_width'],
        min_step=int(item['min_step']) if (item['min_step'].isdigit()) else 0,
        min_step_ramp=int(item['min_step_ramp']) if (item['min_step_ramp'].isdigit()) else 0,
        lift=False if item['lift'] in ['', '0'] else True,
        lift_minus_step=item['lift_minus_step'],
        min_rail_width=int(item['min_rail_width'])/10 if (item['min_rail_width'].isdigit() and item['min_rail_width'] != '0') else None,
        max_rail_width=int(item['max_rail_width'])/10 if (item['max_rail_width'].isdigit() and item['max_rail_width'] != '0') else None,
        max_angle=int(item['max_angle']) if (item['max_angle'].isdigit() and item['max_angle'] != '0') else None,
        escalator=int(item['escalator']) if (item['escalator'].isdigit()) else 0
    )

cities = ['msk','spb','waw','min','kzn','ekb','niz','ams','lau', 'gla', 'vog', 'sam', 'nsk', 'kiev']
datavars = ['lines','stations','portals','interchanges']

LINES = {}
STATIONS = {}
PORTALS = {}
INTERCHANGES = {}
GRAPH = {}
SCHEMAS = {}

for city in cities:
    LINES[city] = [i for i in  csv.DictReader(open(os.path.join(os.path.dirname(__file__),  '../data/%s/lines.csv' % city), 'rb'), delimiter=';')]
    STATIONS[city] = [i for i in  csv.DictReader(open(os.path.join(os.path.dirname(__file__),  '../data/%s/stations.csv' % city), 'rb'), delimiter=';')]
    PORTALS[city] = [i for i in  csv.DictReader(open(os.path.join(os.path.dirname(__file__),  '../data/%s/portals.csv' % city), 'rb'), delimiter=';')]
    INTERCHANGES[city] = [i for i in  csv.DictReader(open(os.path.join(os.path.dirname(__file__),  '../data/%s/interchanges.csv' % city), 'rb'), delimiter=';')]
    GRAPH[city] = init_graph(city)
    schemes = [os.path.basename(n) for n in glob.glob(os.path.join(os.path.dirname(__file__), '../data/%s/schemes/*.png' % city))]
    SCHEMAS[city] = dict(zip([os.path.splitext(s)[0] for s in schemes], schemes))


# Workaround for https://github.com/nextgis/metro4all/issues/217
def by2be(fn):
    def wrapped(*args, **kwargs):
        lang = kwargs['lang']
        kwargs['lang'] = 'be' if lang == 'by' else lang
        return fn(*args, **kwargs)
    return wrapped


@route('/<city>')
@view('index')
def main(city):
    config = {
        'msk': dict(
            mainmap=dict(center=[55.75, 37.62], zoom=10, maxBounds=[[55.391, 37.075], [56.030, 38.188]]),
            city='msk',
            route_css_class='city-1'
        ),
        'spb': dict(
            mainmap=dict(center=[59.95, 30.316667], zoom=10, maxBounds=[[59.744, 30.043], [60.091, 30.567]]),
            city='spb',
            route_css_class='city-2'
        ),
        'waw': dict(
            mainmap=dict(center=[52.2286, 21.0491], zoom=11, maxBounds=[[52.098, 20.852], [52.368, 21.271]]),
            city='waw',
            route_css_class='city-3'
        ),
        'min': dict(
            mainmap=dict(center=[53.916667, 27.55], zoom=11, maxBounds=[[53.805, 27.375], [53.972, 27.831]]),
            city='min',
            route_css_class='city-4'
        ),
        'kzn': dict(
            mainmap=dict(center=[55.7916, 49.1295], zoom=12, maxBounds=[[55.603, 48.821], [55.938, 49.381]]),
            city='kzn',
            route_css_class='city-5'
        ),
        'niz': dict(
            mainmap=dict(center=[56.3004, 43.9165], zoom=12, maxBounds=[[56.193, 43.718], [56.400, 44.135]]),
            city='niz',
            route_css_class='city-6'
        ),
        'ekb': dict(
            mainmap=dict(center=[56.8366, 60.6535], zoom=11, maxBounds=[[56.716, 60.383], [56.945, 60.843]]),
            city='ekb',
            route_css_class='city-7'
        ),
        'ams': dict(
            mainmap=dict(center=[52.3723,4.9013], zoom=11, maxBounds=[[52.163, 4.465], [52.629, 5.347]]),
            city='ams',
            route_css_class='city-8'
         ),
        'lau': dict(
            mainmap=dict(center=[46.5218,6.6327], zoom=13, maxBounds=[[46.478, 6.479], [46.587, 6.758]]),
            city='lau',
            route_css_class='city-9'
         ),
        'vog': dict(
            mainmap=dict(center=[48.7106, 44.5171], zoom=11, maxBounds=[[48.407, 44.109], [48.889, 44.690]]),
            city='vog',
            route_css_class='city-10'
        ),
        'gla': dict(
            mainmap=dict(center=[55.861147,-4.2499891], zoom=13, maxBounds=[[55.668, -4.613], [56.034, -3.935]]),
            city='gla',
            route_css_class='city-11'
         ),
        'sam': dict(
            mainmap=dict(center=[53.2061, 50.2171], zoom=11, maxBounds=[[53.092, 49.747], [53.552, 50.390]]),
            city='sam',
            route_css_class='city-12'
        ),
        'nsk': dict(
            mainmap=dict(center=[55.0241, 82.9248], zoom=11, maxBounds=[[54.7785, 82.6268], [55.1420, 83.3395]]),
            city='nsk',
            route_css_class='city-13'
        ),
        'kiev': dict(
            mainmap=dict(center=[50.4131, 30.5358], zoom=11, maxBounds=[[50.3203, 30.3545], [50.5385, 30.6429]]),
            city='kiev',
            route_css_class='city-14'
        )
    }
    city = city if city in cities else 'msk'
    return dict(config=config[city], request=request)


@route('/static/<path:path>')
def static(path):
    import os
    return static_file(path, root=os.path.join(os.path.dirname(__file__), 'static'))


@route('/data/<path:path>')
def schemes(path):
    return static_file(path, root=os.path.join(os.path.dirname(__file__), '../data'))


# Получение списка станций для выпадающих списков
@route('/<lang>/<city>/stations')
@by2be
def get_stations(lang, city):
    results = []
    for line in LINES[city]:
        group = []
        for station in STATIONS[city]:
            if line['id_line'] == station['id_line']:
                station_json = {
                    'id':   station['id_station'],
                    'text': station.get('name_' + lang, station.get('name_en')),
                    'lon':  station.get('lon'),
                    'lat':  station.get('lat')
                }
                if station['id_station'] in SCHEMAS[city]:
                    station_json['sch'] = SCHEMAS[city][station['id_station']]
                group.append(station_json)
        # group = sorted(group, key=lambda i: i['text'])
        results.append({
            'text': line.get('name_' + lang, line.get('name_en')),
            'color': line.get('color'),
            'children': group
        })

    response.content_type = 'application/json'
    return dumps(dict(results=results))


# Получение списка входов для заданной станции
@route('/<lang>/<city>/portals/search')
@by2be
def get_portals(lang, city):

    id_station = request.query.station

    # ['in', 'out']
    direction = request.query.direction

    portals = []
    for portal in PORTALS[city]:
        d = portal['direction'] if portal['direction'] != '' else 'both'
        if (portal['id_station']) == id_station and d in [direction, 'both']:
            feature = Feature(
                id=portal['id_entrance'],
                geometry=dict(
                    type='Point',
                    coordinates=[float(portal['lon']), float(portal['lat'])]
                ),
                properties=dict(
                    name=portal.get('name_' + lang, portal.get('name_en')),
                    meetcode=portal['meetcode'],
                    direction=portal['direction'],
                    barriers=get_barriers(portal)
                )
            )
            portals.append(feature)

    response.content_type = 'application/json'
    return dumps(FeatureCollection(portals))


@route('/<lang>/<city>/routes/search')
@by2be
def get_routes(lang, city, limit=3):

    station_from = int(request.query.station_from) if request.query.station_from else None
    station_to = int(request.query.station_to) if request.query.station_to else None
    portal_from = int(request.query.portal_from) if request.query.portal_from else None
    portal_to = int(request.query.portal_to) if request.query.portal_to else None

    # Извлечение информации о станции
    def get_station_info(station_id):
        for station in STATIONS[city]:
            if station['id_station'] == str(station_id):
                return dict(
                    name=station.get('name_' + lang, station.get('name_en')),
                    line=int(station['id_line']),
                    coords=(float(station['lat']), float(station['lon'])),
                    node_id=station['id_node']
                )

    # Извлечение информации о линии
    def get_line_info(line_id):
        for line in LINES[city]:
            if line['id_line'] == str(line_id):
                return dict(
                    name=line.get('name_' + lang, line.get('name_en')),
                    color=line['color']
                )

    # Проверка на принадлежность станций к одной линии
    def check_the_same_line(node1, node2):
        node1_line = get_station_info(node1)['line']
        node2_line = get_station_info(node2)['line']
        return node1_line == node2_line

    # Заполнение информации о препятствиях на входах и выходах
    def portal_barriers(portal_id):
        for portal in PORTALS[city]:
            if int(portal['id_entrance']) == portal_id:
                return get_barriers(portal)

    # Заполнение информации о препятствиях на переходах
    def interchange_barriers(station_from, station_to):
        for interchange in INTERCHANGES[city]:
            if (int(interchange['station_from']) == station_from) and (int(interchange['station_to']) == station_to):
                return get_barriers(interchange)

    if ((station_from is not None) and (station_to is not None)):

        all_shortest_paths = nx.all_shortest_paths(
            GRAPH[city],
            station_from,
            station_to,
            weight='weight'
        )

        routes = []
        for index, path in enumerate(all_shortest_paths):
            if index == limit:
                break

            route = []
            for station in path:
                station_info = get_station_info(station)
                line_id = station_info['line']
                same_line = check_the_same_line(station, get_next_item(path, station))

                station_type = "regular" if same_line else "interchange"
                node_id=station_info['node_id']

                unit = dict(
                    id=station,
                    station_type=station_type,
                    station_name=station_info['name'],
                    coordinates=station_info['coords'],
                    station_line=dict(
                        id=line_id,
                        name=get_line_info(line_id)['name'],
                        color=get_line_info(line_id)['color']
                    ),
                    schema=SCHEMAS[city][str(node_id)] if str(node_id) in SCHEMAS[city] else None
                )

                if station_type == "interchange":
                    unit['barriers'] = None
                    unit['barriers'] = interchange_barriers(station, get_next_item(path, station))

                elif station_type == "regular":
                    unit['barriers'] = None

                route.append(unit)

            # Заполняем информацию о входах
            if portal_from is not None:
                portal_from_obj = filter(
                    lambda portal: portal['id_entrance'] == str(portal_from),
                    PORTALS[city]
                )[0]

            if portal_to is not None:
                portal_to_obj = filter(
                    lambda portal: portal['id_entrance'] == str(portal_to),
                    PORTALS[city]
                )[0]

            portals = dict(
                portal_from=dict(
                    barriers=portal_barriers(portal_from),
                    meetcode='#%s' % portal_from_obj['meetcode'],
                    name='%s' % portal_from_obj.get('name_' + lang, portal_from_obj.get('name_en'))
                ) if portal_from else None,
                portal_to=dict(
                    barriers=portal_barriers(portal_to),
                    meetcode='#%s' % portal_to_obj['meetcode'],
                    name='%s' % portal_to_obj.get('name_' + lang, portal_to_obj.get('name_en'))
                ) if portal_to else None
            )

            routes.append(dict(route=route, portals=portals))

        response.content_type = 'application/json'
        return dumps(dict(result=routes))

    else:
        return HTTPResponse(status=400)


app = bottle.default_app()


def get_app():
    return app

if __name__ == "__main__":
    run(host='0.0.0.0', port=8088, server='waitress')
