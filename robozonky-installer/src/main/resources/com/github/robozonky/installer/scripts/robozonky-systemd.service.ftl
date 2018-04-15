[Unit]
Description=RoboZonky: Automated Zonky.cz investing robot
After=network.target

[Service]
User=${data.uid}
Group=${data.gid}
Restart=always
WorkingDirectory=${data.pwd}
ExecStart=${data.script}
ExecStop=

[Install]
WantedBy=multi-user.target
