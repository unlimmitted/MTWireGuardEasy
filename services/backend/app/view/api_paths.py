import os
from typing import Annotated
import jwt
from fastapi import Depends, APIRouter, HTTPException
from fastapi.security import OAuth2PasswordBearer
from starlette import status
from controllers.mikrotik_movements import authenticate_user, MikroTik
from controllers.mikrotik_movements import response, mikrotik_user
from models import authModels, peerModels, errorModels, settingsModels


SECRET_KEY = os.environ["SECRET_KEY"]
ALGORITHM = os.environ["ALGORITHM"]

router = APIRouter()
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/try-connect")


@router.post("/peers")
async def get_peer(verify_token: authModels.VerifyToken):
    mt = MikroTik(verify_token.token)
    if mt.get_api() is True:
        if mt.try_get_settings() is True:
            response['setting_status'] = True
            return mt.list_peers()
        else:
            return response


@router.post("/add-peer")
async def add_peer(peer: peerModels.NewPeerBody):
    mt = MikroTik(peer.token)
    if mt.get_api() is True:
        return mt.create_new_peer(peer)
    return response


@router.delete("/del-peer")
async def del_client(client: peerModels.DelPeerBody):
    mt = MikroTik(client.token)
    if mt.get_api() is True:
        return mt.list_peers(client.id)
    return response


@router.post("/change_vpn_rout")
async def change_vpn_rout(peer_vpn: peerModels.VpnChangeRouteBody):
    mt = MikroTik(peer_vpn.token)
    if mt.get_api() is True:
        mt.change_vpn_route(peer_vpn)
        return mt.list_peers()
    return response


@router.delete('/del-error')
async def del_error(error: errorModels.DelErrorBody):
    response["errors"].pop(error.id)
    return response


@router.post('/set-settings')
async def set_settings(settings: settingsModels.SettingsBody):
    mt = MikroTik(settings.token)
    if mt.get_api() is True:
        return mt.set_settings(settings)
    return response


@router.post("/try-connect", response_model=authModels.Token)
def connect_to_mikrotik(
        form_data: Annotated[authModels.UserAuthForm, Depends()]):
    user = authenticate_user(form_data.host, form_data.username, form_data.password)
    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Connection details are incorrect",
            headers={"WWW-Authenticate": "Bearer"})
    access_token = jwt.encode({"username": form_data.username}, SECRET_KEY, ALGORITHM)
    return {"access_token": access_token, "token_type": "bearer"}


@router.post("/verify-auth-token")
async def check_auth_status(verify_token: authModels.VerifyToken):
    try:
        token_data = jwt.decode(verify_token.token, SECRET_KEY, algorithms=ALGORITHM)
        return mikrotik_user[token_data["username"]]
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            headers={"WWW-Authenticate": "Bearer"})
