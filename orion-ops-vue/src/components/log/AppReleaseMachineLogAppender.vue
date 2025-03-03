<template>
  <div class="app-release-container">
    <!-- 步骤 -->
    <div class="app-release-steps">
      <a-steps :current="current" :status="$enum.valueOf($enum.ACTION_STATUS, detail.status).stepStatus">
        <template v-for="action in detail.actions">
          <a-step :key="action.id"
                  :title="action.actionName"
                  :subTitle="action.used ? `${action.used}ms` : ''">
            <template v-if="action.status === $enum.ACTION_STATUS.RUNNABLE.value" #icon>
              <a-icon type="loading"/>
            </template>
          </a-step>
        </template>
      </a-steps>
    </div>
    <!-- 日志 -->
    <div class="machine-release-log">
      <logAppender ref="appender"
                   size="default"
                   :appendStyle="{height: appenderHeight}"
                   :relId="id"
                   :tailType="$enum.FILE_TAIL_TYPE.APP_RELEASE_LOG.value"
                   :downloadType="$enum.FILE_DOWNLOAD_TYPE.APP_RELEASE_MACHINE_LOG.value">
        <!-- 左侧工具 -->
        <template #left-tools>
          <div class="machine-log-tools">
            <a-tag color="#5C7CFA" v-if="detail.machineName">
              {{ detail.machineName }}
            </a-tag>
            <a-tag color="#40C057" v-if="detail.machineHost">
              {{ detail.machineHost }}
            </a-tag>
            <!-- 停止 -->
            <a-popconfirm v-if="$enum.ACTION_STATUS.RUNNABLE.value === detail.status"
                          title="是否要停止执行?"
                          placement="bottomLeft"
                          ok-text="确定"
                          cancel-text="取消"
                          @confirm="terminatedMachine">
              <a-button icon="close" size="small">停止</a-button>
            </a-popconfirm>
          </div>
        </template>
      </logAppender>
    </div>
  </div>
</template>

<script>

import LogAppender from '@/components/log/LogAppender'

export default {
  name: 'AppReleaseMachineLogAppender',
  components: { LogAppender },
  props: {
    appenderHeight: {
      type: String,
      default: 'calc(100vh - 112px)'
    }
  },
  data() {
    return {
      id: null,
      current: 0,
      detail: {},
      pollId: null
    }
  },
  methods: {
    open(id) {
      this.id = id
      this.$api.getAppReleaseMachineDetail({
        releaseMachineId: this.id
      }).then(({ data }) => {
        this.detail = data
        this.setStepsCurrent()
        // 设置轮询状态
        if (this.detail.status === this.$enum.ACTION_STATUS.WAIT.value ||
          this.detail.status === this.$enum.ACTION_STATUS.RUNNABLE.value) {
          this.pollId = setInterval(this.pollStatus, 2000)
        }
      }).then(() => {
        this.$nextTick(() => this.$refs.appender.openTail())
      })
    },
    close() {
      // 关闭轮询
      if (this.pollId) {
        clearInterval(this.pollId)
        this.pollId = null
      }
      // 关闭tail
      this.$nextTick(() => {
        this.$refs.appender.clear()
        this.$refs.appender.close()
      })
      this.id = null
      this.current = 0
      this.detail = {}
    },
    terminatedMachine() {
      this.$api.terminatedAppReleaseMachine({
        id: this.detail.releaseId,
        releaseMachineId: this.detail.id
      }).then(() => {
        this.$message.success('已停止')
      })
    },
    pollStatus() {
      this.$api.getAppReleaseMachineStatus({
        releaseMachineId: this.id
      }).then(({ data }) => {
        this.detail.status = data.status
        // 清除状态轮询
        if (this.detail.status !== this.$enum.BUILD_STATUS.WAIT.value &&
          this.detail.status !== this.$enum.BUILD_STATUS.RUNNABLE.value) {
          clearInterval(this.pollId)
          this.pollId = null
        }
        // 设置action
        if (!data.actions || !data.actions.length || !this.detail.actions || !this.detail.actions.length) {
          return
        }
        for (const actionStatus of data.actions) {
          this.detail.actions.filter(s => s.id === actionStatus.id).forEach(e => {
            e.status = actionStatus.status
            e.used = actionStatus.used
          })
        }
        // 设置当前操作
        this.setStepsCurrent()
      })
    },
    setStepsCurrent() {
      const len = this.detail.actions.length
      let curr = len - 1
      for (let i = 0; i < len; i++) {
        const status = this.detail.actions[i].status
        if (status !== this.$enum.ACTION_STATUS.FINISH.value) {
          curr = i
          break
        }
      }
      this.current = curr
    }
  },
  beforeDestroy() {
    this.pollId !== null && clearInterval(this.pollId)
    this.pollId = null
  }
}
</script>

<style lang="less" scoped>

.app-release-steps {
  height: 56px;
  padding: 12px 12px 4px 12px;
}

.machine-release-log {
  padding: 8px;

  .machine-log-tools {
    display: flex;
    align-items: baseline;
  }
}

</style>
