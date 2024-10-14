**Frontend : https://github.com/unlimmitted/MTWireGuardEasy-frontend**

MikroTik system requirements: 
1. Only **_MikroTik RouterBoard_** or **_MikroTik CHR_**
2. **_2 Kb_** of free space
3. RouterOS **_not lower 7.15_** 

## **Launch command**
```bash
docker build --tag mtwgeasy . && docker run --name MTWGEasy \
 -d -p 8080:8080 \
 -e GATEWAY=<you-mikrotik-ip> \
 -e MIKROTIK_USER=<you-mikrotik-admin-login> \
 -e MIKROTIK_PASSWORD=<you-mikrotik-pass> \
 -e IP_ROUTE_NAME=WGMTEasy \
 mtwgeasy
```

```
http://localhost:8080
```

## **Settings**
#### **Only wireguard server**
![photo_2024-09-22_17-19-24](https://github.com/user-attachments/assets/956d8d75-caaf-4135-ac1c-fe40fcccb047)

#### **VPN chain mode**
**For the settings to appear, you need to activate the Enable Double WireGuard VPN checkbox**
###### Added a function to import from the WireGuard .conf file
![photo_2024-09-23_19-18-57](https://github.com/user-attachments/assets/db1b7cfb-501a-45e3-b398-2252fd386df1)


## **Appearance:**

###### _Main screen_
![Скриншот сделанный 2024-09-21 в 22 30 11](https://github.com/user-attachments/assets/0ef41b8a-57da-4c79-8c8c-ae82245f43ed)

###### **Peer modal**
![peerModal](https://github.com/user-attachments/assets/578e0438-1879-4757-8443-76f33079d9eb)
