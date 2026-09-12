import { defineConfig } from 'vite';
import scalaJSPlugin from '@scala-js/vite-plugin-scalajs';

export default defineConfig({
  plugins: [
    scalaJSPlugin({
      // The sbt build lives one level up (monorepo root), and the Scala.js
      // module is the `frontend` subproject.
      cwd: '..',
      projectID: 'frontend',
    }),
  ],
  server: {
    port: 5173,
    open: true,
  },
});
