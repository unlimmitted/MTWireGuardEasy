from pydantic import BaseModel


class NewPeerBody(BaseModel):
    comment: str
    token: str | None


class DelPeerBody(BaseModel):
    id: str
    token: str | None


class VpnChangeRouteBody(BaseModel):
    status: bool
    id: str
    token: str | None
