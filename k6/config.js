export const BASE_URL = __ENV.BASE_URL || "https://api.kusitms-mate.cloud";
export const TEST_ID = __ENV.TEST_ID || "1";

export {
  getUserToken,
  getUserTokenByIteration,
  getMakerToken,
  getMakerTokenByIndex,
  getRefreshToken,
  authHeaders,
  tokenPoolSize,
} from "./lib/auth.js";
