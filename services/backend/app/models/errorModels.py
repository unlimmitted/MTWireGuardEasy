from pydantic import BaseModel


class DelErrorBody(BaseModel):
    id: int
    token: str | None
