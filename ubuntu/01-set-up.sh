# User and directories
sudo useradd --system --home /opt/dbtest --shell /usr/sbin/nologin dbtest || true
sudo mkdir -p /opt/dbtest
sudo chown -R dbtest:dbtest /opt/dbtest
sudo mkdir -p /opt/dbtest/config
sudo chown -R dbtest:dbtest /opt/dbtest/config

##########################
# Copying
##########################

# app.jar
sudo cp -f /home/paveltaruts/app.jar /opt/dbtest/app.jar
sudo chown dbtest:dbtest /opt/dbtest/app.jar

# config/application.yaml
sudo cp -f /home/paveltaruts/application.yaml /opt/dbtest/config/
sudo chown -R dbtest:dbtest /opt/dbtest/config

# dbtest.service
sudo cp ~/dbtest.service /etc/systemd/system/
