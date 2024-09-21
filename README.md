Front : https://github.com/unlimmitted/MTWireGuardEasy-frontend

MikroTik system requirements: 
1. Only RoterBOARD
2. 500 bytes of free space in the root
3. RouterOS not lower 7.15 

```cpp
docker build --tag mtwgeasy .
```

```cpp
docker run --name MTWGEasy -d --no-cache -p 8080:8080 -e GATEWAY=<you-mikrotik-ip> -e MIKROTIK_USER=<you-mikrotik-admin-login> -e MIKROTIK_PASSWORD=<you-mikrotik-pass> mtwgeasy
```

![Скриншот сделанный 2024-09-21 в 22 30 11](https://github.com/user-attachments/assets/0ef41b8a-57da-4c79-8c8c-ae82245f43ed)

![peerModal](https://github.com/user-attachments/assets/578e0438-1879-4757-8443-76f33079d9eb)
