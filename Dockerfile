FROM clojure

COPY . /usr/src/app
WORKDIR /usr/src/app
RUN lein deps
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" xml-splitter.jar
CMD ["java", "-jar", "xml-splitter.jar"]