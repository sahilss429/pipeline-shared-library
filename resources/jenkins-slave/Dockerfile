FROM jenkins/inbound-agent:4.3-4-alpine
ENV HELM_VERSION 3.5.0
ENV ARGO_VERSION v2.0.3
ENV BINARY yq_linux_amd64
ENV YQ_VERSION v4.2.0
USER root
RUN mkdir /root/.ssh
COPY config /root/.ssh/

RUN apk add --update perl-utils docker make curl ansible openrc python3 libc6-compat\
    py3-pip findutils jq gcc g++ libc-dev pkgconfig \
    libxml2-dev libxslt-dev postgresql-dev coreutils wget bash \
    gnupg tar linux-headers bison readline-dev readline zlib-dev \
    zlib yaml-dev autoconf ncurses-dev curl-dev apache2-dev \
    libx11-dev libffi-dev tcl-dev tk-dev
RUN curl -s https://cache.ruby-lang.org/pub/ruby/2.7/ruby-2.7.0.tar.gz | tar -zx -C /var/cache/apk/ \
    && cd /var/cache/apk/ruby-2.7.0 && ./configure --disable-install-doc && make && make install 
RUN gem install rake puppet-blacksmith \
    && pip3 install --upgrade pip \
    && pip3 install awscli boto \
    && rm -rf /var/cache/apk/*
RUN rc-update add docker boot
# install helm
RUN mkdir /tmp/helm                   \
 && wget https://github.com/mikefarah/yq/releases/download/${YQ_VERSION}/${BINARY} -O /usr/bin/yq \
 &&  chmod +x /usr/bin/yq \
 && curl -L https://raw.githubusercontent.com/warrensbox/terraform-switcher/release/install.sh | bash \
 && wget https://github.com/zegl/kube-score/releases/download/v1.11.0/kube-score_1.11.0_linux_amd64.tar.gz \
 && tar xvzf kube-score_1.11.0_linux_amd64.tar.gz \
 && mv kube-score /usr/local/bin/ \
 && rm -rf LICENSE kube-score_1.11.0_linux_amd64.tar.gz \
 && cd /tmp/helm                    \
 && curl -sSL -o /usr/local/bin/argocd https://github.com/argoproj/argo-cd/releases/download/$ARGO_VERSION/argocd-linux-amd64 \
 && curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh -s -- -b /usr/local/bin v0.18.3\
 && curl -o /usr/local/bin/aws-iam-authenticator https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/aws-iam-authenticator \
 && curl -s https://get.helm.sh/helm-v${HELM_VERSION}-linux-amd64.tar.gz | tar zxvf - \
 && cp /tmp/helm/linux-amd64/helm /usr/local/bin/helm  \
 && chmod +x /usr/local/bin/helm \
 && chmod 755 /usr/local/bin/aws-iam-authenticator \
 && chmod +x /usr/local/bin/argocd \
 && rm -rf /tmp/helm 
RUN helm plugin install --version 0.2.7 https://github.com/quintush/helm-unittest
RUN mkdir -p ~/.terraform.d/plugins && \
 curl -Ls https://api.github.com/repos/gavinbunney/terraform-provider-kubectl/releases/latest \
 | jq -r ".assets[] | select(.browser_download_url | contains(\"$(uname -s | tr A-Z a-z)\")) | select(.browser_download_url | contains(\"amd64\")) | .browser_download_url" \
 | xargs -n 1 curl -Lo ~/.terraform.d/plugins/terraform-provider-kubectl.zip && \
 cd ~/.terraform.d/plugins/ && \
 unzip ~/.terraform.d/plugins/terraform-provider-kubectl.zip -d /tmp && \
 mv /tmp/terraform-provider-kubectl* . && \
 chmod +x terraform-provider-kubectl* && \
 rm -rf terraform-provider-kubectl-tmp && \
 rm -rf terraform-provider-kubectl.zip
USER root
