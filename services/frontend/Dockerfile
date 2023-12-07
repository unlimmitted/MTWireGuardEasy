FROM node:lts-alpine

RUN npm install -g http-server

WORKDIR /app/MTWireGuardEasy-frontend/
COPY package*.json ./

RUN npm install vite
RUN npm install

COPY . .

RUN npm run build

EXPOSE 8080
CMD [ "http-server", "dist" ]