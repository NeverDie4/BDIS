<script>
export default {
  onLaunch() {
    console.log('BDIS mobile app launched')
  }
}
</script>

<style>
page {
  min-height: 100%;
  background: #f5f7fb;
  color: #1f2937;
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, Arial, sans-serif;
  font-size: 28rpx;
}

.page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 28rpx;
  background: #f5f7fb;
}

.card {
  box-sizing: border-box;
  margin-bottom: 24rpx;
  padding: 28rpx;
  border-radius: 16rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.06);
}

.title {
  color: #111827;
  font-size: 38rpx;
  font-weight: 700;
  line-height: 1.35;
}

.sub-title {
  margin-top: 10rpx;
  color: #64748b;
  font-size: 26rpx;
  line-height: 1.5;
}

.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
  padding: 14rpx 0;
}

.label {
  flex-shrink: 0;
  color: #64748b;
}

.value {
  min-width: 0;
  color: #111827;
  font-weight: 500;
  text-align: right;
  word-break: break-all;
}

.primary-btn,
.secondary-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  margin: 18rpx 0 0;
  border-radius: 12rpx;
  font-size: 30rpx;
  line-height: 88rpx;
}

.primary-btn {
  color: #ffffff;
  background: #1677ff;
}

.secondary-btn {
  color: #1677ff;
  background: #eef5ff;
}

.empty {
  padding: 48rpx 20rpx;
  color: #94a3b8;
  text-align: center;
}

.tag {
  display: inline-flex;
  align-items: center;
  min-height: 42rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  color: #1677ff;
  background: #eef5ff;
  font-size: 24rpx;
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
  border-radius: 12rpx;
  background: #f8fafc;
  color: #111827;
}

.textarea {
  min-height: 160rpx;
}
</style>
