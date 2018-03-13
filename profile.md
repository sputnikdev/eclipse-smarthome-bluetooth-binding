## Remote profiling

### Setting up JMS for VisualVM

Add this in OH `/usr/share/openhab2/runtime/binsetenv` script:
```sh
EXTRA_JAVA_OPTS_COMMON="-Djava.rmi.server.hostname=192.168.0.2 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9199 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false ..."
```

Set up ssh to forward the port:

```sh
ssh -v -D 9696 user@hostname

or

ssh -N -f -L 9199:localhost:9199 user@hostname
```

Add a JMX connector in VisualVM for localhost:9199

### Setting up jstadt for VisualVM

On the OH server run jstatd

```sh
jstatd -J-Djava.rmi.server.logCalls=true -p 1099 -J-Djava.rmi.sver.hostname=192.168.0.2 -J-Djava.security.policy=<(echo 'grant codebase "file:${java.home}/../lib/tools.jar" {permission java.security.AllPermission;};')
```

In Visual VM add a new "Remote Host" for 192.168.0.2:1099