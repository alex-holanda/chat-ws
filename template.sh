#! /bin/bash
sudo yum update -y
sudo yum -y install docker
sudo service docker start
sudo usermod -a -G docker ec2-user
sudo chkconfig docker on
sudo docker pull alexholanda/curso-chat-ws
sudo docker run --name redis -d -p 6379:6379 --restart always --env ALLOW_EMPTY_PASSWORD=yes redis:7.0-alpine
sudo docker run --name curso-chat-ws -d -p 80:80 --restart always --env SERVER_PORT=80 --env REDIS_HOST=redis --env REDIS_PORT=6379 --env MONGODB_URI='' --link redis alexholanda/curso-chat-ws:latest
