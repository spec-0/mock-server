/** @type {import('next').NextConfig} */
const nextConfig = {
  // Static export so Spring Boot can serve the built files from src/main/resources/static/ui/
  output: 'export',
  // The standalone server serves the UI at /ui
  basePath: '/ui',
  // Disable image optimisation (not available in static export)
  images: { unoptimized: true },
  trailingSlash: true,
};

module.exports = nextConfig;
