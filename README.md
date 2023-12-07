# MTWireGuardEasy

##Install
Cloning this repository
```console
git clone https://github.com/unlimmitted/MTWireGuardEasy.git
```

Cloning frontend
```console
cd MTWireGuardEasy/services/frontend/
```
```console
git clone https://github.com/unlimmitted/MTWireGuardEasy-frontend.git
```

Set your computer's IP address in serverUrl

```console
cd MTWireGuardEasy-frontend/src/pages/
```

```console
nano Main.vue
```

![image](https://github.com/unlimmitted/MTWireGuardEasy/assets/108941648/5458be9a-ea27-44a1-adf1-b28b8d29c3a2)

##Run
To run the application, run the command in the root directory:
```console
sudo docker-compose up -d --build
```

##Update

In root directory:
```console
git pull
```
```console
sudo docker-compose up -d --build
```
