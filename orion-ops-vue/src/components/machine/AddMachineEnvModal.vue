<template>
  <a-modal v-model="visible"
           v-drag-modal
           :title="title"
           :width="450"
           :okButtonProps="{props: {disabled: loading}}"
           :maskClosable="false"
           :destroyOnClose="true"
           @ok="check"
           @cancel="close">
    <a-spin :spinning="loading">
      <a-form :form="form" v-bind="layout">
        <a-form-item label="key" v-show="id == null">
          <a-input v-decorator="decorators.key" allowClear/>
        </a-form-item>
        <a-form-item label="value" style="margin-bottom: 12px;">
          <a-textarea v-decorator="decorators.value" allowClear/>
        </a-form-item>
        <a-form-item label="描述" style="margin-bottom: 0;">
          <a-textarea v-decorator="decorators.description" allowClear/>
        </a-form-item>
      </a-form>
    </a-spin>
  </a-modal>
</template>

<script>

import { pick } from 'lodash'

const layout = {
  labelCol: { span: 5 },
  wrapperCol: { span: 17 }
}

function getDecorators() {
  return {
    key: ['key', {
      rules: [{
        required: true,
        message: '请输入key'
      }, {
        max: 128,
        message: 'key长度不能大于128位'
      }]
    }],
    value: ['value', {
      rules: [{
        required: true,
        message: '请输入value'
      }, {
        max: 512,
        message: 'value长度不能大于512位'
      }]
    }],
    description: ['description', {
      rules: [{
        max: 64,
        message: '描述长度不能大于64位'
      }]
    }]
  }
}

export default {
  name: 'AddMachineEnvModal',
  data: function() {
    return {
      id: null,
      visible: false,
      title: null,
      loading: false,
      record: null,
      machineId: null,
      layout,
      decorators: getDecorators.call(this),
      form: this.$form.createForm(this)
    }
  },
  methods: {
    add(machineId) {
      this.title = '新增变量'
      this.machineId = machineId
      this.initRecord({})
    },
    update(id) {
      this.title = '修改变量'
      this.$api.getMachineEnvDetail({ id })
        .then(({ data }) => {
          this.initRecord(data)
        })
    },
    initRecord(row) {
      this.form.resetFields()
      this.visible = true
      this.id = row.id
      this.record = pick(Object.assign({}, row), 'key', 'value', 'description')
      this.$nextTick(() => {
        this.form.setFieldsValue(this.record)
      })
    },
    check() {
      this.loading = true
      this.form.validateFields((err, values) => {
        if (err) {
          this.loading = false
          return
        }
        this.submit(values)
      })
    },
    async submit(values) {
      let res
      try {
        if (!this.id) {
          // 添加
          res = await this.$api.addMachineEnv({
            ...values,
            machineId: this.machineId
          })
        } else {
          // 修改
          res = await this.$api.updateMachineEnv({
            ...values,
            id: this.id
          })
        }
        if (!this.id) {
          this.$message.success('添加成功')
          this.$emit('added', res.data)
        } else {
          this.$message.success('修改成功')
          this.$emit('updated', res.data)
        }
        this.close()
      } catch (e) {
        // ignore
      }
      this.loading = false
    },
    close() {
      this.visible = false
      this.loading = false
      this.machineId = null
    }
  }
}
</script>

<style scoped>

</style>
