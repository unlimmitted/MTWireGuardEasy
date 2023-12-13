FROM node:lts-alpine as build-stage

RUN apk add --no-cache git
RUN git clone https://github.com/unlimmitted/MTWireGuardEasy-frontend.git

WORKDIR /MTWireGuardEasy-frontend
COPY package*.json ./

RUN npm install vite
RUN npm install
COPY . .
RUN npm run build

FROM python:3.11 as production-stage

COPY --from=build-stage /MTWireGuardEasy-frontend/dist /templates

RUN mkdir /code
WORKDIR /code

COPY ./requirements.txt /code/requirements.txt
RUN pip install --upgrade pip
RUN pip install --no-cache-dir --upgrade -r /code/requirements.txt
COPY . .

CMD [ "python3", "-m" , "flask", "run", "--host=0.0.0.0"]