<template>
  <div class="app-info-container">
    <!-- 搜索列 -->
    <div class="table-search-columns">
      <a-form-model class="app-info-search-form" ref="query" :model="query">
        <a-row>
          <a-col :span="5">
            <a-form-model-item label="应用" prop="appId">
              <AppSelector ref="appSelector" @change="appId => query.appId = appId"/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="标题" prop="title">
              <a-input v-model="query.title" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="状态" prop="status">
              <a-select v-model="query.status" placeholder="请选择" allowClear>
                <a-select-option v-for="status of $enum.RELEASE_STATUS" :key="status.value" :value="status.value">
                  {{ status.label }}
                </a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
          <a-col :span="5">
            <a-form-model-item label="描述" prop="description">
              <a-input v-model="query.description" allowClear/>
            </a-form-model-item>
          </a-col>
          <a-col :span="4">
            <a-form-model-item label="只看自己" prop="onlyMyself">
              <a-checkbox v-model="query.onlyMyself" @change="getList({})"/>
            </a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
    </div>
    <!-- 工具栏 -->
    <div class="table-tools-bar">
      <!-- 左侧 -->
      <div class="tools-fixed-left">
        <span class="table-title">发布列表</span>
        <a-divider v-show="selectedRowKeys.length" type="vertical"/>
        <div v-show="selectedRowKeys.length">
          <a-popconfirm title="确认删除所选中的发布记录吗?"
                        placement="topRight"
                        ok-text="确定"
                        cancel-text="取消"
                        @confirm="remove(selectedRowKeys)">
            <a-button class="ml8" type="danger" icon="delete">删除</a-button>
          </a-popconfirm>
        </div>
      </div>
      <!-- 右侧 -->
      <div class="tools-fixed-right">
        <a-button v-if="query.profileId" class="ml16 mr8" type="primary" icon="deployment-unit" @click="openRelease">应用发布</a-button>
        <a-divider type="vertical"/>
        <a-icon type="search" class="tools-icon" title="查询" @click="getList({})"/>
        <a-icon type="reload" class="tools-icon" title="重置" @click="resetForm"/>
      </div>
    </div>
    <!-- 表格 -->
    <div class="table-main-container">
      <a-table :columns="columns"
               :dataSource="rows"
               :pagination="pagination"
               :rowSelection="rowSelection"
               rowKey="id"
               @change="getList"
               @expand="expandMachine"
               :loading="loading"
               :expandedRowKeys.sync="expandedRowKeys"
               size="middle">
        <!-- 展开的机器列表 -->
        <template v-slot:expandedRowRender="record">
          <a-table
            v-if="record.machines"
            :rowKey="(record, index) => index"
            :columns="innerColumns"
            :dataSource="record.machines"
            :loading="record.loading"
            :pagination="false"
            size="middle">
            <!-- 状态 -->
            <template v-slot:status="machine">
              <a-tag class="m0" :color="$enum.valueOf($enum.ACTION_STATUS, machine.status).color">
                {{ $enum.valueOf($enum.ACTION_STATUS, machine.status).label }}
              </a-tag>
            </template>
            <!-- 操作 -->
            <template v-slot:action="machine">
              <!-- 日志 -->
              <a-button class="p0"
                        type="link"
                        style="height: 22px"
                        :disabled="!statusHolder.visibleActionLog(machine.status)">
                <a-tooltip title="ctrl 点击打开新页面">
                  <a target="_blank"
                     :href="`#/app/release/machine/log/view/${machine.id}`"
                     @click="openMachineLog($event, machine.id)">日志</a>
                </a-tooltip>
              </a-button>
              <a-divider type="vertical"/>
              <!-- 详情 -->
              <a @click="openMachineDetail(machine.id)">详情</a>
              <a-divider type="vertical" v-if="statusHolder.visibleMachineTerminated(machine.status)"/>
              <!-- 停止 -->
              <a-popconfirm v-if="statusHolder.visibleMachineTerminated(machine.status)"
                            title="是否要停止执行?"
                            placement="topRight"
                            ok-text="确定"
                            cancel-text="取消"
                            @confirm="terminatedMachine(record.id, machine.id)">
                <span class="span-blue pointer">停止</span>
              </a-popconfirm>
              <a-divider type="vertical" v-if="statusHolder.visibleMachineSkip(record.status, machine.status)"/>
              <!-- 跳过 -->
              <a-popconfirm v-if="statusHolder.visibleMachineSkip(record.status, machine.status)"
                            title="是否要跳过执行?"
                            placement="topRight"
                            ok-text="确定"
                            cancel-text="取消"
                            @confirm="skipMachine(record.id, machine.id)">
                <span class="span-blue pointer">跳过</span>
              </a-popconfirm>
            </template>
          </a-table>
        </template>
        <!-- 构建序列 -->
        <template v-slot:seq="record">
          <a-tag class="ml8" color="#5C7CFA">
            #{{ record.buildSeq }}
          </a-tag>
        </template>
        <!-- 发布标题 -->
        <template v-slot:releaseTitle="record">
          <div class="timed-wrapper">
            <!-- 定时图标 -->
            <a-tooltip v-if="record.timedRelease === $enum.TIMED_TYPE.TIMED.value">
              <template #title>
                调度时间: {{ record.timedReleaseTime | formatDate }}
              </template>
              <a-icon class="timed-icon" type="hourglass"/>
            </a-tooltip>
            <!-- 标题 -->
            {{ record.title }}
          </div>
        </template>
        <!-- 状态 -->
        <template v-slot:status="record">
          <a-tag class="m0" :color="$enum.valueOf($enum.RELEASE_STATUS, record.status).color">
            {{ $enum.valueOf($enum.RELEASE_STATUS, record.status).label }}
          </a-tag>
        </template>
        <!-- 创建时间 -->
        <template v-slot:createTime="record">
          {{ record.createTime | formatDate }}
        </template>
        <!-- 操作 -->
        <template v-slot:action="record">
          <!-- 审核 -->
          <a v-if="statusHolder.visibleAudit(record.status)" @click="openAudit(record.id)" title="审核">审核</a>
          <a-divider type="vertical" v-if="statusHolder.visibleAudit(record.status)"/>
          <!-- 复制 -->
          <a-popconfirm v-if="statusHolder.visibleCopy(record.status)"
                        title="是否要复制发布任务?"
                        placement="topRight"
                        ok-text="确定"
                        cancel-text="取消"
                        @confirm="copyRelease(record.id)">
            <span class="span-blue pointer">复制</span>
          </a-popconfirm>
          <a-divider type="vertical" v-if="statusHolder.visibleCopy(record.status)"/>
          <!-- 发布 -->
          <a-popconfirm v-if="statusHolder.visibleRelease(record.status)"
                        title="是否要执行发布?"
                        placement="topRight"
                        ok-text="确定"
                        cancel-text="取消"
                        @confirm="runnableRelease(record)">
            <span class="span-blue pointer">发布</span>
          </a-popconfirm>
          <a-divider type="vertical" v-if="statusHolder.visibleRelease(record.status)"/>
          <!-- 定时 -->
          <a v-if="statusHolder.visibleTimed(record.status)" @click="openTimedRelease(record)" title="设置定时发布">定时</a>
          <a-divider type="vertical" v-if="statusHolder.visibleTimed(record.status)"/>
          <!-- 停止 -->
          <a-popconfirm v-if="statusHolder.visibleTerminated(record.status)"
                        title="是否要停止发布?"
                        placement="topRight"
                        ok-text="确定"
                        cancel-text="取消"
                        @confirm="terminated(record.id)">
            <span class="span-blue pointer">停止</span>
          </a-popconfirm>
          <a-divider type="vertical" v-if="statusHolder.visibleTerminated(record.status)"/>
          <!-- 回滚 -->
          <a-popconfirm v-if="statusHolder.visibleRollback(record.status)"
                        title="是否要回滚发布?"
                        placement="topRight"
                        ok-text="确定"
                        cancel-text="取消"
                        @confirm="rollback(record.id)">
            <span class="span-blue pointer">回滚</span>
          </a-popconfirm>
          <a-divider type="vertical" v-if="statusHolder.visibleRollback(record.status)"/>
          <!-- 日志 -->
          <a-tooltip v-if="statusHolder.visibleLog(record.status)" title="ctrl 点击打开新页面">
            <a target="_blank"
               :href="`#/app/release/log/view/${record.id}`"
               @click="openReleaseLog($event, record.id)">日志</a>
          </a-tooltip>
          <a-divider v-if="statusHolder.visibleLog(record.status)" type="vertical"/>
          <!-- 详情 -->
          <a @click="openReleaseDetail(record.id)">详情</a>
          <!-- 取消 -->
          <a-divider type="vertical" v-if="statusHolder.visibleCancel(record.status)"/>
          <a-popconfirm v-if="statusHolder.visibleCancel(record.status)"
                        title="是否要取消定时发布?"
                        placement="topRight"
                        ok-text="确定"
                        cancel-text="取消"
                        @confirm="cancelTimedRelease(record)">
            <span class="span-blue pointer">取消</span>
          </a-popconfirm>
          <!-- 删除 -->
          <a-divider v-if="statusHolder.visibleDelete(record.status)" type="vertical"/>
          <a-popconfirm v-if="statusHolder.visibleDelete(record.status)"
                        title="确认删除当前发布记录吗?"
                        placement="topRight"
                        ok-text="确定"
                        cancel-text="取消"
                        @confirm="remove([record.id])">
            <span class="span-blue pointer">删除</span>
          </a-popconfirm>
        </template>
      </a-table>
    </div>
    <!-- 事件 -->
    <div class="app-info-event">
      <!-- 添加模态框 -->
      <AppReleaseModal ref="addModal" @submit="getList({})"/>
      <!-- 审核模态框 -->
      <AppReleaseAuditModal ref="auditModal" @audit="releaseAudit"/>
      <!-- 发布详情抽屉 -->
      <AppReleaseDetailDrawer ref="releaseDetail"/>
      <!-- 机器详情抽屉 -->
      <AppReleaseMachineDetailDrawer ref="machineDetail"/>
      <!-- 发布日志 -->
      <AppReleaseLogAppenderModal ref="releaseAppender"/>
      <!-- 机器日志 -->
      <AppReleaseMachineLogAppenderModal ref="machineAppender"/>
      <!-- 定时模态框 -->
      <AppReleaseTimedModal ref="releaseTimed" @updated="forceUpdateRows"/>
    </div>
  </div>
</template>

<script>
import AppSelector from '@/components/app/AppSelector'
import AppReleaseModal from '@/components/app/AppReleaseModal'
import AppReleaseAuditModal from '@/components/app/AppReleaseAuditModal'
import AppReleaseTimedModal from '@/components/app/AppReleaseTimedModal'
import AppReleaseDetailDrawer from '@/components/app/AppReleaseDetailDrawer'
import AppReleaseMachineDetailDrawer from '@/components/app/AppReleaseMachineDetailDrawer'
import AppReleaseMachineLogAppenderModal from '@/components/log/AppReleaseMachineLogAppenderModal'
import _filters from '@/lib/filters'
import AppReleaseLogAppenderModal from '@/components/log/AppReleaseLogAppenderModal'

function statusHolder() {
  return {
    visibleCopy: (status) => {
      return true
    },
    visibleCancel: (status) => {
      return status === this.$enum.RELEASE_STATUS.WAIT_SCHEDULE.value
    },
    visibleAudit: (status) => {
      return (status === this.$enum.RELEASE_STATUS.WAIT_AUDIT.value ||
        status === this.$enum.RELEASE_STATUS.AUDIT_REJECT.value) && this.$isAdmin()
    },
    visibleRelease: (status) => {
      return status === this.$enum.RELEASE_STATUS.WAIT_RUNNABLE.value ||
        status === this.$enum.RELEASE_STATUS.WAIT_SCHEDULE.value
    },
    visibleTimed: (status) => {
      return status === this.$enum.RELEASE_STATUS.WAIT_RUNNABLE.value ||
        status === this.$enum.RELEASE_STATUS.WAIT_SCHEDULE.value
    },
    visibleTerminated: (status) => {
      return status === this.$enum.RELEASE_STATUS.RUNNABLE.value
    },
    visibleDelete: (status) => {
      return status !== this.$enum.RELEASE_STATUS.RUNNABLE.value &&
        status !== this.$enum.RELEASE_STATUS.WAIT_SCHEDULE.value
    },
    visibleRollback: (status) => {
      return status === this.$enum.RELEASE_STATUS.FINISH.value
    },
    visibleLog: (status) => {
      return status === this.$enum.RELEASE_STATUS.RUNNABLE.value ||
        status === this.$enum.RELEASE_STATUS.FINISH.value ||
        status === this.$enum.RELEASE_STATUS.FAILURE.value ||
        status === this.$enum.RELEASE_STATUS.TERMINATED.value
    },
    visibleActionLog: (status) => {
      return status === this.$enum.ACTION_STATUS.RUNNABLE.value ||
        status === this.$enum.ACTION_STATUS.FINISH.value ||
        status === this.$enum.ACTION_STATUS.FAILURE.value ||
        status === this.$enum.ACTION_STATUS.TERMINATED.value
    },
    visibleMachineTerminated: (status) => {
      return status === this.$enum.ACTION_STATUS.RUNNABLE.value
    },
    visibleMachineSkip: (status, machineStatus) => {
      return status === this.$enum.RELEASE_STATUS.RUNNABLE.value &&
        machineStatus === this.$enum.ACTION_STATUS.WAIT.value
    }
  }
}

/**
 * 列
 */
const columns = [
  {
    title: '构建序列',
    key: 'seq',
    width: 100,
    sorter: (a, b) => a.buildSeq - b.buildSeq,
    scopedSlots: { customRender: 'seq' }
  },
  {
    title: '标题',
    key: 'title',
    ellipsis: true,
    scopedSlots: { customRender: 'releaseTitle' }
  },
  {
    title: '发布应用',
    key: 'appName',
    dataIndex: 'appName',
    ellipsis: true,
    sorter: (a, b) => a.appName.localeCompare(b.appName)
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    align: 'center',
    sorter: (a, b) => a.status - b.status,
    scopedSlots: { customRender: 'status' }
  },
  {
    title: '持续时间',
    key: 'keepTime',
    dataIndex: 'keepTime',
    width: 100,
    sorter: (a, b) => (a.used || 0) - (b.used || 0)
  },
  {
    title: '创建时间',
    key: 'createTime',
    align: 'center',
    ellipsis: true,
    width: 160,
    sorter: (a, b) => a.createTime - b.createTime,
    scopedSlots: { customRender: 'createTime' }
  },
  {
    title: '描述',
    dataIndex: 'description',
    key: 'description',
    ellipsis: true
  },
  {
    title: '操作',
    key: 'action',
    width: 260,
    scopedSlots: { customRender: 'action' }
  }
]

/**
 * 展开的机器列
 */
const innerColumns = [
  {
    title: '机器名称',
    key: 'name',
    dataIndex: 'machineName',
    width: 200,
    ellipsis: true,
    sorter: (a, b) => a.machineName.localeCompare(b.machineName)
  },
  {
    title: '机器主机',
    key: 'host',
    dataIndex: 'machineHost',
    width: 180,
    ellipsis: true,
    sorter: (a, b) => a.machineHost.localeCompare(b.machineHost)
  },
  {
    title: '状态',
    key: 'status',
    width: 120,
    align: 'center',
    sorter: (a, b) => a.status - b.status,
    scopedSlots: { customRender: 'status' }
  },
  {
    title: '持续时间',
    key: 'keepTime',
    dataIndex: 'keepTime',
    width: 120,
    sorter: (a, b) => (a.used || 0) - (b.used || 0)
  },
  {
    title: '操作',
    key: 'action',
    width: 140,
    align: 'center',
    scopedSlots: { customRender: 'action' }
  }
]

export default {
  name: 'AppRelease',
  components: {
    AppReleaseLogAppenderModal,
    AppReleaseTimedModal,
    AppReleaseMachineLogAppenderModal,
    AppReleaseMachineDetailDrawer,
    AppReleaseDetailDrawer,
    AppReleaseAuditModal,
    AppReleaseModal,
    AppSelector
  },
  data: function() {
    return {
      query: {
        appId: null,
        profileId: null,
        title: null,
        status: undefined,
        description: null,
        onlyMyself: false
      },
      rows: [],
      pagination: {
        current: 1,
        pageSize: 10,
        total: 0,
        showTotal: function(total) {
          return `共 ${total} 条`
        }
      },
      loading: false,
      expandedRowKeys: [],
      pollId: null,
      columns,
      innerColumns,
      selectedRowKeys: [],
      statusHolder: statusHolder.call(this)
    }
  },
  computed: {
    rowSelection() {
      return {
        selectedRowKeys: this.selectedRowKeys,
        columnWidth: '40px',
        onChange: e => {
          this.selectedRowKeys = e
        },
        getCheckboxProps: record => ({
          props: {
            disabled: record.status === this.$enum.RELEASE_STATUS.RUNNABLE.value ||
              record.status === this.$enum.RELEASE_STATUS.WAIT_SCHEDULE.value
          }
        })
      }
    }
  },
  methods: {
    chooseProfile(profile) {
      this.query.profileId = profile.id
      this.getList({})
    },
    getList(page = this.pagination) {
      this.loading = true
      this.expandedRowKeys = []
      this.$api.getAppReleaseList({
        ...this.query,
        onlyMyself: this.query.onlyMyself ? 1 : 2,
        page: page.current,
        limit: page.pageSize
      }).then(({ data }) => {
        const pagination = { ...this.pagination }
        pagination.total = data.total
        pagination.current = data.page
        this.$utils.defineArrayKey(data.rows, 'loading', false)
        this.$utils.defineArrayKey(data.rows, 'machines', [])
        this.rows = data.rows || []
        this.pagination = pagination
        this.selectedRowKeys = []
        this.loading = false
      }).catch(() => {
        this.loading = false
      })
    },
    expandMachine(expand, record) {
      if (!expand || record.machines.length) {
        return
      }
      // 加载机器
      record.loading = true
      this.$api.getAppReleaseMachineList({
        id: record.id
      }).then(({ data }) => {
        record.loading = false
        record.machines = data
      }).catch(() => {
        record.loading = false
      })
    },
    openRelease() {
      this.$refs.addModal.openRelease(this.query.profileId)
    },
    openAudit(id) {
      this.$refs.auditModal.open(id)
    },
    releaseAudit(id, res) {
      const match = this.rows.filter(s => s.id === id)
      if (match && match.length) {
        match[0].status = (res ? this.$enum.RELEASE_STATUS.WAIT_RUNNABLE.value : this.$enum.RELEASE_STATUS.AUDIT_REJECT.value)
      }
    },
    copyRelease(id) {
      this.$api.copyAppRelease({
        id
      }).then(() => {
        this.$message.success('已复制')
        this.getList({})
      })
    },
    cancelTimedRelease(record) {
      this.$api.cancelAppTimedRelease({
        id: record.id
      }).then(() => {
        this.$message.success('已取消定时发布')
        record.status = this.$enum.RELEASE_STATUS.WAIT_RUNNABLE.value
        record.timedRelease = this.$enum.TIMED_TYPE.NORMAL.value
        record.timedReleaseTime = undefined
        // 强制刷新状态
        this.forceUpdateRows()
      })
    },
    runnableRelease(record) {
      this.$api.runnableAppRelease({
        id: record.id
      }).then(() => {
        this.$message.success('已提交执行请求')
        record.status = this.$enum.RELEASE_STATUS.RUNNABLE.value
      })
    },
    openTimedRelease(record) {
      this.$refs.releaseTimed.open(record)
    },
    terminated(id) {
      this.$api.terminatedAppRelease({
        id
      }).then(() => {
        this.$message.success('已提交停止请求')
      })
    },
    rollback(id) {
      this.$api.rollbackAppRelease({
        id
      }).then(() => {
        this.$message.success('已提交回滚请求')
        this.getList({})
      })
    },
    remove(idList) {
      this.$api.deleteAppRelease({
        idList
      }).then(() => {
        this.$message.success('已删除')
        this.getList({})
      })
    },
    openReleaseLog(e, id) {
      if (!e.ctrlKey) {
        e.preventDefault()
        // 打开模态框
        this.$refs.releaseAppender.open(id)
        return false
      } else {
        // 跳转页面
        return true
      }
    },
    openReleaseDetail(id) {
      this.$refs.releaseDetail.open(id)
    },
    openMachineLog(e, id) {
      if (!e.ctrlKey) {
        e.preventDefault()
        // 打开模态框
        this.$refs.machineAppender.open(id)
        return false
      } else {
        // 跳转页面
        return true
      }
    },
    openMachineDetail(id) {
      this.$refs.machineDetail.open(id)
    },
    terminatedMachine(id, releaseMachineId) {
      this.$api.terminatedAppReleaseMachine({
        id,
        releaseMachineId
      }).then(() => {
        this.$message.success('已停止')
      })
    },
    skipMachine(id, releaseMachineId) {
      this.$api.skipAppReleaseMachine({
        id,
        releaseMachineId
      }).then(() => {
        this.$message.success('已跳过')
      })
    },
    resetForm() {
      this.$refs.query.resetFields()
      this.$refs.appSelector.reset()
      this.query.appId = undefined
      this.query.status = undefined
      this.query.onlyMyself = false
      this.getList({})
    },
    forceUpdateRows() {
      this.$set(this.rows, 0, this.rows[0])
    },
    pollStatus() {
      if (!this.rows || !this.rows.length) {
        return
      }
      const pollItems = this.rows.filter(r => r.status === this.$enum.RELEASE_STATUS.WAIT_AUDIT.value ||
        r.status === this.$enum.RELEASE_STATUS.AUDIT_REJECT.value ||
        r.status === this.$enum.RELEASE_STATUS.WAIT_RUNNABLE.value ||
        r.status === this.$enum.RELEASE_STATUS.WAIT_SCHEDULE.value ||
        r.status === this.$enum.RELEASE_STATUS.RUNNABLE.value)
      if (!pollItems.length) {
        return
      }
      const idList = pollItems.map(s => s.id)
      if (!idList.length) {
        return
      }
      const machineIdList = pollItems.map(s => s.machines)
        .filter(s => s && s.length)
        .flat()
        .map(s => s.id)
      this.$api.getAppReleaseListStatus({
        idList,
        machineIdList
      }).then(({ data }) => {
        if (!data || !data.length) {
          return
        }
        for (const status of data) {
          // 发布状态
          this.rows.filter(s => s.id === status.id).forEach(row => {
            row.status = status.status
            row.keepTime = status.keepTime
            row.used = status.used
            if (!status.machines || !status.machines.length || !row.machines || !row.machines.length) {
              return
            }
            // 机器状态
            for (const machine of status.machines) {
              row.machines.filter(m => m.id === machine.id).forEach(m => {
                m.status = machine.status
                m.keepTime = machine.keepTime
                m.used = machine.used
              })
            }
          })
        }
        // 强制刷新状态
        this.forceUpdateRows()
      })
    }
  },
  filters: {
    ..._filters
  },
  mounted() {
    // 读取当前环境
    const activeProfile = this.$storage.get(this.$storage.keys.ACTIVE_PROFILE)
    if (!activeProfile) {
      this.$message.warning('请先维护应用环境')
      return
    }
    this.query.profileId = JSON.parse(activeProfile).id
    // 设置轮询
    this.pollId = setInterval(this.pollStatus, 5000)
    // 查询列表
    this.getList({})
  },
  beforeDestroy() {
    this.pollId !== null && clearInterval(this.pollId)
    this.pollId = null
  }
}
</script>

<style lang="less" scoped>

.timed-wrapper {
  display: contents;

  .timed-icon {
    background: #03A9F4;
    color: #FAFAFA;
    font-size: 16px;
    padding: 4px;
    border-radius: 3px;
    margin-right: 2px;
    display: inline-block;
  }
}

/deep/ .ant-table-expand-icon-th, /deep/ .ant-table-row-expand-icon-cell {
  width: 45px;
  min-width: 45px;
}

</style>
