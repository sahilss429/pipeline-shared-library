FROM "jenkins/jenkins:2.289.2-lts-alpine"
USER root
RUN apk update \
	&& apk upgrade --available && sync \
	&& apk add curl busybox-extras bind-tools
USER jenkins
