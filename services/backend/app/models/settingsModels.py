from pydantic import BaseModel


class SettingsBody(BaseModel):
    localWgNetwork: str
    localWgEndpoint: str
    localWgEndpointPort: str
    localNetwork: str
    ipAddress: str
    allowedAddress: str
    endpoint: str
    endpointPort: str
    publicKey: str
    privateKey: str
    presharedKey: str
    wanInterfaceName: str
    portForward: bool
    token: str | None
