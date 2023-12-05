from pydantic import BaseModel


class UserAuthForm(BaseModel):
    host: str
    username: str
    password: str


class Token(BaseModel):
    access_token: str
    token_type: str


class VerifyToken(BaseModel):
    token: str | None
