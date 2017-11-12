description "RoboZonky: Automated Zonky.cz investing robot"

respawn
setuid ${data.uid}
setgid ${data.gid}

chdir ${data.pwd}

script
    ${data.script}
end script
