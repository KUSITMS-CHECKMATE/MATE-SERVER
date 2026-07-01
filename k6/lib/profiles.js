/**
 * k6 부하 프로필
 * K6_PROFILE=smoke|load|stress 환경변수로 선택 (기본: load)
 */
const PROFILES = {
  smoke: [
    { duration: "30s", target: 10 },
    { duration: "30s", target: 0 },
  ],
  medium: [
    { duration: "1m", target: 50 },
    { duration: "2m", target: 100 },
    { duration: "1m", target: 0 },
  ],
  ramp200: [
    { duration: "1m", target: 100 },
    { duration: "2m", target: 200 },
    { duration: "1m", target: 0 },
  ],
  ramp300: [
    { duration: "1m", target: 150 },
    { duration: "2m", target: 300 },
    { duration: "1m", target: 0 },
  ],
  ramp500: [
    { duration: "2m", target: 250 },
    { duration: "2m", target: 500 },
    { duration: "1m", target: 0 },
  ],
  load: [
    { duration: "2m", target: 500 },
    { duration: "3m", target: 1000 },
    { duration: "1m", target: 0 },
  ],
  stress: [
    { duration: "1m", target: 500 },
    { duration: "2m", target: 1000 },
    { duration: "3m", target: 1000 },
    { duration: "1m", target: 0 },
  ],
};

export function rampStages(profile) {
  const name = profile || __ENV.K6_PROFILE || "load";
  return PROFILES[name] || PROFILES.load;
}

export function rampScenario(scenarioName, profile) {
  return {
    [scenarioName]: {
      executor: "ramping-vus",
      startVUs: 0,
      stages: rampStages(profile),
      gracefulRampDown: "30s",
    },
  };
}
