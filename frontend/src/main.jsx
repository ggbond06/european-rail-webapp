import React, { useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import './styles.css';

const sampleRoutes = [
  ['Amsterdam', 'Vienna'],
  ['London', 'Rome'],
  ['Barcelona', 'Berlin'],
  ['Paris', 'Milan'],
];

const cityCoordinates = {
  'A Coruña': [43.3623, -8.4115],
  Aachen: [50.7753, 6.0839],
  Alicante: [38.3452, -0.481],
  Amsterdam: [52.3676, 4.9041],
  Antwerp: [51.2194, 4.4025],
  Ashford: [51.1465, 0.875],
  Augsburg: [48.3705, 10.8978],
  Avignon: [43.9493, 4.8055],
  Barcelona: [41.3874, 2.1686],
  Basel: [47.5596, 7.5886],
  Berlin: [52.52, 13.405],
  Birmingham: [52.4862, -1.8904],
  Bologna: [44.4949, 11.3426],
  Bonn: [50.7374, 7.0982],
  Bordeaux: [44.8378, -0.5792],
  Braga: [41.5454, -8.4265],
  Bratislava: [48.1486, 17.1077],
  Bregenz: [47.5031, 9.7471],
  Bremen: [53.0793, 8.8017],
  Brescia: [45.5416, 10.2118],
  Bristol: [51.4545, -2.5879],
  Brno: [49.1951, 16.6068],
  Brussels: [50.8503, 4.3517],
  Budapest: [47.4979, 19.0402],
  Burgos: [42.3439, -3.6969],
  Coimbra: [40.2033, -8.4103],
  Cologne: [50.9375, 6.9603],
  Córdoba: [37.8882, -4.7794],
  Dijon: [47.322, 5.0415],
  Dortmund: [51.5136, 7.4653],
  Dresden: [51.0504, 13.7373],
  Düsseldorf: [51.2277, 6.7735],
  Ebbsfleet: [51.4436, 0.3203],
  Edinburgh: [55.9533, -3.1883],
  Erfurt: [50.9848, 11.0299],
  Faro: [37.0194, -7.9304],
  Florence: [43.7696, 11.2558],
  Frankfurt: [50.1109, 8.6821],
  Freiburg: [47.999, 7.8421],
  'Gdańsk': [54.352, 18.6466],
  Geneva: [46.2044, 6.1432],
  Glasgow: [55.8642, -4.2518],
  Granada: [37.1773, -3.5986],
  Graz: [47.0707, 15.4395],
  Grenoble: [45.1885, 5.7245],
  Halle: [51.4969, 11.9688],
  Hamburg: [53.5511, 9.9937],
  Hanover: [52.3759, 9.732],
  Innsbruck: [47.2692, 11.4041],
  Karlsruhe: [49.0069, 8.4037],
  Kassel: [51.3127, 9.4797],
  Katowice: [50.2649, 19.0238],
  Klagenfurt: [46.6365, 14.3122],
  Kraków: [50.0647, 19.945],
  Lausanne: [46.5197, 6.6323],
  'Le Mans': [48.0061, 0.1996],
  Leeds: [53.8008, -1.5491],
  Leipzig: [51.3397, 12.3731],
  León: [42.5987, -5.5671],
  Lille: [50.6292, 3.0573],
  Linz: [48.3069, 14.2858],
  Lisbon: [38.7223, -9.1393],
  Liverpool: [53.4084, -2.9916],
  Liège: [50.6326, 5.5797],
  London: [51.5072, -0.1276],
  Luxembourg: [49.6116, 6.1319],
  Lyon: [45.764, 4.8357],
  Madrid: [40.4168, -3.7038],
  Manchester: [53.4808, -2.2426],
  Mannheim: [49.4875, 8.466],
  Marseille: [43.2965, 5.3698],
  Metz: [49.1193, 6.1757],
  Milan: [45.4642, 9.19],
  Montpellier: [43.6108, 3.8767],
  Munich: [48.1351, 11.582],
  Murcia: [37.9922, -1.1307],
  Málaga: [36.7213, -4.4214],
  Nancy: [48.6921, 6.1844],
  Nantes: [47.2184, -1.5536],
  Naples: [40.8518, 14.2681],
  Newcastle: [54.9783, -1.6178],
  Nice: [43.7102, 7.262],
  Nuremberg: [49.4521, 11.0767],
  Ourense: [42.3358, -7.8639],
  Padua: [45.4064, 11.8768],
  Paris: [48.8566, 2.3522],
  Perpignan: [42.6887, 2.8948],
  Porto: [41.1579, -8.6291],
  Poznań: [52.4064, 16.9252],
  Prague: [50.0755, 14.4378],
  'Reggio Emilia AV': [44.6983, 10.6312],
  Reims: [49.2583, 4.0317],
  Rennes: [48.1173, -1.6778],
  Rome: [41.9028, 12.4964],
  Rotterdam: [51.9244, 4.4777],
  Salerno: [40.6824, 14.7681],
  Salzburg: [47.8095, 13.055],
  Santiago: [42.8782, -8.5448],
  Segovia: [40.9429, -4.1088],
  Seville: [37.3891, -5.9845],
  Strasbourg: [48.5734, 7.7521],
  Stuttgart: [48.7758, 9.1829],
  Toledo: [39.8628, -4.0273],
  Toulouse: [43.6047, 1.4442],
  Tours: [47.3941, 0.6848],
  Trieste: [45.6495, 13.7768],
  Turin: [45.0703, 7.6869],
  Ulm: [48.4011, 9.9876],
  Valencia: [39.4699, -0.3763],
  Valladolid: [41.6523, -4.7245],
  Venice: [45.4408, 12.3155],
  Verona: [45.4384, 10.9916],
  Vienna: [48.2082, 16.3738],
  Vigo: [42.2406, -8.7207],
  Warsaw: [52.2297, 21.0122],
  Wrocław: [51.1079, 17.0385],
  Zaragoza: [41.6488, -0.8891],
  Zurich: [47.3769, 8.5417],
};

function formatDuration(minutes) {
  if (minutes === null || minutes === undefined) return '0 min';
  const hours = Math.floor(minutes / 60);
  const mins = Math.round(minutes % 60);
  if (hours === 0) return `${mins} min`;
  if (mins === 0) return `${hours} hr`;
  return `${hours} hr ${mins} min`;
}

function formatPrice(price) {
  if (price === null || price === undefined) return '€0.00';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'EUR',
  }).format(price);
}

function todayString() {
  const today = new Date();
  today.setMinutes(today.getMinutes() - today.getTimezoneOffset());
  return today.toISOString().slice(0, 10);
}

function App() {
  const travelDateInputRef = useRef(null);
  const [locations, setLocations] = useState([]);
  const [start, setStart] = useState('Amsterdam');
  const [end, setEnd] = useState('Vienna');
  const [travelDate, setTravelDate] = useState(todayString());
  const [pathResult, setPathResult] = useState(null);
  const [closestInput, setClosestInput] = useState('Amsterdam, Paris, Berlin');
  const [closestResult, setClosestResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    fetch('/api/locations')
      .then((response) => response.json())
      .then((data) => {
        setLocations(data.locations ?? []);
      })
      .catch(() => {
        setError('Could not load rail cities from the Java backend.');
      });
  }, []);

  const routeSegments = useMemo(() => {
    if (!pathResult?.path?.length) return [];
    return pathResult.path.slice(0, -1).map((city, index) => ({
      from: city,
      to: pathResult.path[index + 1],
      minutes: pathResult.times[index],
      price: pathResult.prices?.[index] ?? 0,
    }));
  }, [pathResult]);

  async function requestJson(url) {
    const response = await fetch(url);
    const payload = await response.json();
    if (!response.ok) throw new Error(payload.error || 'Request failed.');
    return payload;
  }

  async function findShortestPath(nextStart = start, nextEnd = end, nextDate = travelDate) {
    setLoading(true);
    setError('');
    setPathResult(null);
    try {
      const params = new URLSearchParams({ start: nextStart, end: nextEnd, date: nextDate });
      const result = await requestJson(`/api/shortest-path?${params}`);
      setStart(nextStart);
      setEnd(nextEnd);
      setTravelDate(nextDate);
      setPathResult(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  async function findClosest() {
    setLoading(true);
    setError('');
    setClosestResult(null);
    try {
      const params = new URLSearchParams({ from: closestInput });
      setClosestResult(await requestJson(`/api/closest?${params}`));
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function handleTravelDateChange(event) {
    const nextDate = event.currentTarget.value;
    setTravelDate(nextDate);
    if (pathResult && nextDate) {
      findShortestPath(start, end, nextDate);
    }
  }

  useEffect(() => {
    if (locations.length > 0 && pathResult === null) {
      findShortestPath('Amsterdam', 'Vienna');
    }
  }, [locations]);

  return (
    <main className="app-shell">
      <section className="hero-band">
        <div className="hero-copy">
          <p className="eyebrow">Dijkstra powered rail planner</p>
          <h1>European Rail Navigator</h1>
          <p className="lede">
            Search the rail graph from <code>europeanRail.dot</code>, trace the fastest directed route,
            and estimate date-sensitive fares between cities.
          </p>
        </div>
        <RailMap path={pathResult?.path ?? []} locations={locations} />
      </section>

      <section className="planner-grid" aria-label="Rail planner tools">
        <div className="tool-panel">
          <div className="panel-heading">
            <span>Shortest Path</span>
            <small>{locations.length} cities loaded</small>
          </div>
          <div className="field-row">
            <label>
              Start
              <input list="cities" value={start} onChange={(event) => setStart(event.target.value)} />
            </label>
            <label>
              Destination
              <input list="cities" value={end} onChange={(event) => setEnd(event.target.value)} />
            </label>
            <label>
              Travel date
              <input
                ref={travelDateInputRef}
                type="date"
                min={todayString()}
                value={travelDate}
                onChange={handleTravelDateChange}
                onInput={handleTravelDateChange}
              />
            </label>
            <button
              type="button"
              onClick={() => findShortestPath(start, end, travelDateInputRef.current?.value || travelDate)}
              disabled={loading}
            >
              Find Route
            </button>
          </div>
          <div className="quick-routes" aria-label="Sample routes">
            {sampleRoutes.map(([from, to]) => (
              <button
                key={`${from}-${to}`}
                type="button"
                onClick={() => findShortestPath(from, to, travelDateInputRef.current?.value || travelDate)}
              >
                {from} to {to}
              </button>
            ))}
          </div>
        </div>

        <div className="tool-panel">
          <div className="panel-heading">
            <span>Closest Meeting City</span>
            <small>Comma-separated starts</small>
          </div>
          <div className="meeting-row">
            <label>
              Starting cities
              <input
                value={closestInput}
                onChange={(event) => setClosestInput(event.target.value)}
                placeholder="Amsterdam, Paris, Berlin"
              />
            </label>
            <button type="button" onClick={findClosest} disabled={loading}>
              Find Meeting City
            </button>
          </div>
        </div>
      </section>

      {error && <p className="notice" role="alert">{error}</p>}

      <section className="results-layout" aria-label="Results">
        <div className="result-panel">
          <div className="result-title">
            <span>Route</span>
            <strong>
              {pathResult
                ? `${formatDuration(pathResult.totalMinutes)} · ${formatPrice(pathResult.totalPriceEuros)}`
                : 'Waiting'}
            </strong>
          </div>
          {pathResult ? (
            <>
              <p className="pricing-note">
                Cached fare estimate for {pathResult.travelDate}. Live checkout fares vary by operator,
                availability, train, and ticket type.
              </p>
              <ol className="path-list">
                {pathResult.path.map((city, index) => (
                  <li key={`${city}-${index}`}>
                    <span>{city}</span>
                    {index < pathResult.times.length && (
                      <small>
                        {formatDuration(pathResult.times[index])} · {formatPrice(pathResult.prices?.[index] ?? 0)}
                      </small>
                    )}
                  </li>
                ))}
              </ol>
              <div className="segment-table">
                {routeSegments.map((segment) => (
                  <div key={`${segment.from}-${segment.to}`}>
                    <span className="segment-from">{segment.from}</span>
                    <span className="segment-to">{segment.to}</span>
                    <strong className="segment-time">{formatDuration(segment.minutes)}</strong>
                    <strong className="segment-price">{formatPrice(segment.price)}</strong>
                  </div>
                ))}
              </div>
            </>
          ) : (
            <p className="empty-state">Choose a start and destination to calculate a route.</p>
          )}
        </div>

        <div className="result-panel">
          <div className="result-title">
            <span>Meeting Point</span>
            <strong>{closestResult ? closestResult.closest : 'Ready'}</strong>
          </div>
          {closestResult ? (
            <div className="meeting-result">
              <p>
                {closestResult.closest} minimizes total travel time from {closestResult.starts.join(', ')}.
              </p>
              <strong>{formatDuration(closestResult.totalMinutes)} combined</strong>
            </div>
          ) : (
            <p className="empty-state">Enter several cities to find the closest shared destination.</p>
          )}
        </div>
      </section>

      <datalist id="cities">
        {locations.map((city) => (
          <option value={city} key={city} />
        ))}
      </datalist>
    </main>
  );
}

function RailMap({ path, locations }) {
  const mapElementRef = useRef(null);
  const mapRef = useRef(null);
  const layerRef = useRef(null);
  const shown = path.length ? path : ['Amsterdam', 'Cologne', 'Frankfurt', 'Munich', 'Vienna'];
  const routePoints = shown
    .map((city) => ({ city, coordinates: cityCoordinates[city] }))
    .filter((point) => point.coordinates);

  useEffect(() => {
    if (!mapElementRef.current || mapRef.current) return;

    mapRef.current = L.map(mapElementRef.current, {
      zoomControl: true,
      scrollWheelZoom: false,
      attributionControl: true,
    }).setView([48.6, 7.8], 5);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 18,
      attribution: '&copy; OpenStreetMap contributors',
    }).addTo(mapRef.current);

    layerRef.current = L.layerGroup().addTo(mapRef.current);

    return () => {
      mapRef.current?.remove();
      mapRef.current = null;
      layerRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!mapRef.current || !layerRef.current) return;

    layerRef.current.clearLayers();
    const latLngs = routePoints.map((point) => point.coordinates);

    if (latLngs.length === 0) return;

    if (latLngs.length > 1) {
      L.polyline(latLngs, {
        color: '#147c75',
        weight: 5,
        opacity: 0.9,
        lineCap: 'round',
        lineJoin: 'round',
      }).addTo(layerRef.current);
    }

    routePoints.forEach((point, index) => {
      L.marker(point.coordinates, {
        icon: L.divIcon({
          className: 'route-marker',
          html: `<span>${index + 1}</span>`,
          iconSize: [30, 30],
          iconAnchor: [15, 15],
        }),
      })
        .bindTooltip(point.city, {
          permanent: true,
          direction: 'top',
          offset: [0, -14],
          className: 'route-tooltip',
        })
        .addTo(layerRef.current);
    });

    const bounds = L.latLngBounds(latLngs);
    mapRef.current.fitBounds(bounds.pad(0.35), { animate: false, maxZoom: 7 });
  }, [shown.join('|')]);

  return (
    <figure className="rail-map" aria-label="Route map preview">
      <div ref={mapElementRef} className="leaflet-route-map" role="img" aria-label="Real map route preview" />
      <figcaption>
        <span>{path.length ? 'Current shortest route' : 'Example route preview'}</span>
        <strong>{locations.length || 0} stations</strong>
      </figcaption>
    </figure>
  );
}

createRoot(document.getElementById('root')).render(<App />);
