description "RoboZonky: Automated Zonky.cz investing robot"
console none
respawn
setuid ${data.uid}
setgid ${data.gid}
stop on runlevel [06]
chdir ${data.pwd}
exec ${data.script}
