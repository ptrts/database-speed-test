sudo systemctl daemon-reload
sudo systemctl enable --now dbtest
sudo systemctl stop dbtest
sudo systemctl start dbtest
sudo systemctl restart dbtest
