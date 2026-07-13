<script>
export default {
  onLaunch() {
    console.log('BDIS mobile app launched')
  }
}
</script>

<style>
page {
  --herb-primary: #0f5132;
  --herb-primary-deep: #0b3d2a;
  --herb-button: #166534;
  --herb-button-active: #14532d;
  --herb-green-soft: #eaf5ee;
  --herb-green-border: #b7d7c2;
  --herb-paper: #f7f1e6;
  --herb-paper-light: #fbf7ef;
  --herb-card-soft: #fffaf2;
  --herb-text: #1f2933;
  --herb-text-green: #0f3d2e;
  --herb-text-muted: #64748b;
  --herb-text-warm: #7c6f5c;
  --herb-border: #eadfcd;
  --mobile-tabbar-height: 112rpx;
  min-height: 100%;
  overflow-x: hidden;
  background: #f7f1e6;
  color: #1f2933;
  font-family: 'Noto Serif SC', 'Songti SC', 'STSong', -apple-system, BlinkMacSystemFont,
    'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
}

.page {
  min-height: 100vh;
  box-sizing: border-box;
  overflow-x: hidden;
  padding: 24rpx 24rpx calc(260rpx + env(safe-area-inset-bottom));
  background: linear-gradient(180deg, #fbf7ef 0%, #f7f1e6 100%);
}

/* H5 上 hideTabBar 未及时生效时，避免原生底栏与页面胶囊底栏同时出现。 */
.uni-tabbar-bottom {
  display: none !important;
}

.uni-page-head {
  border-bottom: 1rpx solid #eadfcd !important;
  background: #fffaf2 !important;
  box-shadow: none !important;
}

.uni-page-head__title {
  color: #0f3d2e !important;
}

.card {
  box-sizing: border-box;
  margin-bottom: 24rpx;
  padding: 28rpx;
  border: 1rpx solid #eadfcd;
  border-radius: 16rpx;
  background: #ffffff;
  box-shadow: 0 10rpx 28rpx rgba(63, 45, 24, 0.06);
}

.title,
.sub-title {
  display: block;
}

.title {
  color: #0f3d2e;
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.35;
}

.sub-title {
  margin-top: 10rpx;
  color: #7c6f5c;
  font-size: 26rpx;
  line-height: 1.5;
}

.row,
.info-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24rpx;
  padding: 10rpx 0;
}

.label,
.info-label {
  width: 150rpx;
  flex-shrink: 0;
  color: #7c6f5c;
  font-size: 26rpx;
}

.value,
.info-value {
  min-width: 0;
  flex: 1;
  color: #1f2933;
  font-size: 28rpx;
  font-weight: 500;
  text-align: right;
  overflow-wrap: anywhere;
  word-break: break-word;
}

button.primary-btn,
button.secondary-btn,
.uni-button.primary-btn,
.uni-button.secondary-btn {
  display: flex;
  height: 88rpx;
  align-items: center;
  justify-content: center;
  margin: 18rpx 0 0;
  border: 1rpx solid transparent;
  border-radius: 16rpx;
  font-size: 30rpx;
  font-weight: 600;
  line-height: 88rpx;
}

button.primary-btn,
.uni-button.primary-btn {
  background: linear-gradient(135deg, #166534 0%, #0f5132 100%) !important;
  background-color: #166534 !important;
  box-shadow: 0 8rpx 18rpx rgba(15, 81, 50, 0.18);
  color: #ffffff !important;
}

button.secondary-btn,
.uni-button.secondary-btn {
  border-color: #b7d7c2 !important;
  background: #eaf5ee !important;
  background-color: #eaf5ee !important;
  color: #0f5132 !important;
}

button.primary-btn:active,
button.secondary-btn:active,
.uni-button.primary-btn:active,
.uni-button.secondary-btn:active {
  opacity: 0.88;
}

button.primary-btn[disabled],
button.secondary-btn[disabled],
.uni-button.primary-btn[disabled],
.uni-button.secondary-btn[disabled] {
  border-color: #e5e7eb !important;
  background: #f3f4f6 !important;
  box-shadow: none;
  color: #9ca3af !important;
}

button.primary-btn::after,
button.secondary-btn::after,
.uni-button.primary-btn::after,
.uni-button.secondary-btn::after {
  border: 0;
}

.empty {
  padding: 48rpx 20rpx;
  color: #7c6f5c;
  text-align: center;
}

.tag,
.status-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8rpx 18rpx;
  border-radius: 999rpx;
  background: #e8f7ed;
  color: #15803d;
  font-size: 24rpx;
  font-weight: 500;
}

.form-item {
  margin-bottom: 24rpx;
}

.input,
.textarea {
  box-sizing: border-box;
  width: 100%;
  margin-top: 12rpx;
  padding: 20rpx 22rpx;
  border: 1rpx solid #eadfcd;
  border-radius: 12rpx;
  background: #fffaf2;
  color: #1f2933;
}

.input:focus-within,
.textarea:focus-within {
  border-color: #0f5132;
}

.textarea {
  min-height: 160rpx;
}

.section-title {
  display: flex;
  align-items: center;
  color: #0f3d2e;
  font-size: 30rpx;
  font-weight: 700;
}

.section-title::before {
  width: 8rpx;
  height: 30rpx;
  margin-right: 14rpx;
  border-radius: 999rpx;
  background: #166534;
  content: '';
}

.status-published,
.status-in_progress,
.status-collecting,
.status-completed,
.status-confirmed,
.batch-status-collecting,
.batch-status-confirmed {
  background: #e8f7ed !important;
  color: #15803d !important;
}

.status-pending,
.status-reviewing,
.batch-status-submitted,
.batch-status-reviewing {
  background: #fff4df !important;
  color: #d97706 !important;
}

.status-rejected,
.status-failed,
.batch-status-rejected {
  background: #fee2e2 !important;
  color: #dc2626 !important;
}

.status-cancelled,
.batch-status-cancelled {
  background: #f3f4f6 !important;
  color: #6b7280 !important;
}

.status-archived,
.batch-status-archived {
  background: #f4ead8 !important;
  color: #8a5a2b !important;
}
</style>
