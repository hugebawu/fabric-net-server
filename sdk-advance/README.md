The e2e_cli originally comes from fabric-v1.3/examples/e2e_cli.
To run it in the fabric-v1.4.0 environment, i have revised its 
config.yaml file according to the config.yaml file in fabric-sdk-java-v1.4.0, 
and put it into fabric-v1.4.0/examples/. 

To run e2e_cli, after preparing basic environment of fabric-v1.4.0, Just run 

$ `./network_setup.sh restart` 

It works!

To furthermore develop fabric java app, I have revised the paths in the config files of e2e_cli,
and put it in sdk-advance/src/main/resources.

pay attention to:
 
the $PROJECT path in the start-channel.sh and the MAIN_HOME path in the wiz


