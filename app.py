import os

import jwt
from flask import Flask, render_template, request, send_file, url_for, send_from_directory

from controllers.mikrotik_movements import authenticate_user, MikroTik
from controllers.mikrotik_movements import response, mikrotik_user

app = Flask(__name__, static_folder='templates/static')
app.config['SEND_FILE_MAX_AGE_DEFAULT'] = 0

SECRET_KEY = os.environ["SECRET_KEY"]
ALGORITHM = os.environ["ALGORITHM"]


@app.route('/')
def render_frontend():
    return render_template('index.html')


@app.route('/api/v1/peers', methods=['POST'])
def get_peers():
    data = request.get_json()
    mt = MikroTik(data.get("token"))
    if mt.get_api() is True:
        if mt.try_get_settings() is True:
            response['setting_status'] = True
            return mt.list_peers()
        return response
    return "UNAUTHORIZED", 401


@app.route("/api/v1/add-peer", methods=['POST'])
def add_peer():
    peer = request.get_json()
    mt = MikroTik(peer.get("token"))
    if mt.get_api() is True:
        return mt.create_new_peer(peer)
    return "UNAUTHORIZED", 401


@app.route("/api/v1/del-peer", methods=["DELETE"])
def del_client():
    peer = request.get_json()
    mt = MikroTik(peer.get("token"))
    if mt.get_api() is True:
        return mt.list_peers(peer.get("id"))
    return "UNAUTHORIZED", 401


@app.route("/api/v1/change_vpn_rout", methods=['POST'])
def change_vpn_rout():
    peer = request.get_json()
    mt = MikroTik(peer.get("token"))
    if mt.get_api() is True:
        mt.change_vpn_route(peer)
        return mt.list_peers()
    return "UNAUTHORIZED", 401


@app.route('/api/v1/del-error', methods=["DELETE"])
def del_error():
    error = request.get_json()
    response["errors"].pop(error.get("id"))
    return response


@app.route('/api/v1/set-settings', methods=['POST'])
def set_settings():
    settings = request.get_json()
    mt = MikroTik(settings.get("token"))
    if mt.get_api() is True:
        return mt.set_settings(settings)
    return "UNAUTHORIZED", 401


@app.route('/api/v1/try-connect', methods=['POST'])
def try_connect():
    form_data = request.args
    user = authenticate_user(form_data.get("host"), form_data.get("username"), form_data.get("password"))
    if not user:
        return "UNAUTHORIZED", 401
    access_token = jwt.encode({"username": form_data.get("username")}, SECRET_KEY, ALGORITHM)
    return {"access_token": access_token, "token_type": "bearer"}


@app.route("/api/v1/download-config", methods=['POST'])
def download_config():
    config = request.get_json()
    with open('config.conf', 'w') as f:
        f.write(config.get("value"))
    return send_file(path_or_file='config.conf', download_name='config.conf', mimetype='multipart/form-data')


@app.route('/favicon.png')
def favicon():
    return send_from_directory(os.path.join(app.root_path, 'templates/dist/'),
                               'favicon.png', mimetype='image/vnd.microsoft.icon')


if __name__ == '__main__':
    app.run()
