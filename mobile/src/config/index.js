const config = {
  baseUrl: process.env.NODE_ENV === 'development' ? '' : 'http://localhost:8080',
  mobilePrefix: '/api/mobile/herb',
  timeout: 30000
}

export default config
