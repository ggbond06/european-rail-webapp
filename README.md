# European Rail Navigator

A Java + React web app for finding shortest train routes through the `europeanRail.dot`
graph. The backend loads the DOT data and runs Dijkstra's algorithm; the frontend
renders the planner and a real OpenStreetMap route preview.

Each graph edge includes `minutes` and `price_eur`. The price values are stored
as estimated standard adult second-class segment fares in euros, because live
European rail fares vary by operator, booking date, train, class, availability,
and fare type.

## Project Layout

```text
backend/
  src/main/java/      Java server, graph, backend, and interfaces
  src/test/java/      Java tests and placeholders
  lib/                Optional local test jars
data/
  europeanRail.dot    Rail graph database
frontend/
  src/                React source
  dist/               Built frontend served by Java
  package.json        React/Vite dependencies
scripts/
  build.sh            Build frontend and compile Java
  run.sh              Start the Java web server
```

## Run

From the project root:

```bash
cd frontend
npm install
npm run build
cd ..
javac -d backend/build/classes backend/src/main/java/*.java
java -cp backend/build/classes WebApp 8000
```

Then open:

```text
http://localhost:8000
```

## Useful Scripts

```bash
./scripts/build.sh
./scripts/run.sh 8000
```

## Tests

The tests use JUnit Jupiter. If `backend/lib/junit-platform-console-standalone-1.13.0-M3.jar`
is present, run them from the project root with:

```bash
javac -cp backend/lib/junit-platform-console-standalone-1.13.0-M3.jar -d backend/build/test-classes backend/src/main/java/*.java backend/src/test/java/*.java
java -jar backend/lib/junit-platform-console-standalone-1.13.0-M3.jar --class-path backend/build/test-classes --scan-class-path
```
