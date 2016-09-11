FROM transgressoft/debian-java8-maven

WORKDIR /code

# Add sources, download dependencies, compile

ADD . /code/RandomFileCopierFX

RUN apt-get install xvfb
RUN Xvfb :99 &>/dev/null &
RUN export DISPLAY=:99
RUN sh -e /etc/init.d/xvfb start

# docker run -v /tmp/.X10-unix:/tmp/.X11-unix -e DISPLAY=unix$DISPLAY {image_name}
