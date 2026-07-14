import { initializeApp } from "firebase-admin/app";
import { setGlobalOptions } from "firebase-functions/v2";

import { firebaseRegion } from "./config";

initializeApp();
setGlobalOptions({ region: firebaseRegion });

export {
  ravelryImportPatternById,
  ravelryImportPatternByUrl,
  ravelrySearchPatterns,
} from "./ravelry/patternImport";
export {
  ravelryAuthStatus,
  ravelryCallback,
  ravelryCurrentUser,
  ravelryDisconnect,
  ravelryStartAuth,
} from "./ravelry/auth";
