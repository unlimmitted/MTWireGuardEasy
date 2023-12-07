FROM node:lts-alpine

WORKDIR /app
RUN apk add --no-cache git
RUN git clone https://github.com/unlimmitted/MTWireGuardEasy-frontend.git

RUN npm install -g http-server

WORKDIR /app/MTWireGuardEasy-frontend/
COPY package*.json ./

RUN npm install vite
RUN npm install

COPY . .

RUN npm run build

EXPOSE 8080
CMD [ "http-server", "dist" ]