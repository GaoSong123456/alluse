<template>
  <a-form :form="form" :rules="rules" @submit="handleSubmit">
    <!-- 是否自动获取角色 -->
    <a-form-item label="是否自动获取角色" prop="isAutoGetRole">
      <a-select v-decorator="['isAutoGetRole']" placeholder="请选择">
        <a-select-option value="0">否</a-select-option>
        <a-select-option value="1">是</a-select-option>
      </a-select>
    </a-form-item>

    <!-- 角色来源（isAutoGetRole为1时必填） -->
    <a-form-item label="角色来源" prop="roleOrigin">
      <a-input v-decorator="['roleOrigin']" placeholder="请输入角色来源" />
    </a-form-item>

    <!-- 组织角色（isAutoGetRole为0时必填） -->
    <a-form-item label="组织角色" prop="orgRole">
      <a-input v-decorator="['orgRole']" placeholder="请输入组织角色" />
    </a-form-item>

    <a-form-item>
      <a-button type="primary" html-type="submit">提交</a-button>
    </a-form-item>
  </a-form>
</template>

<script>
import { Form, Select, Input, Button } from 'ant-design-vue';

export default {
  components: {
    'a-form': Form,
    'a-form-item': Form.Item,
    'a-select': Select,
    'a-select-option': Select.Option,
    'a-input': Input,
    'a-button': Button
  },
  data() {
    // 自定义校验：角色来源（isAutoGetRole=1时必填）
    const validateRoleOrigin = (rule, value, callback) => {
      const { isAutoGetRole } = this.form.getFieldsValue();
      if (isAutoGetRole === '1' && !value) {
        callback(new Error('当自动获取角色为"是"时，角色来源不能为空'));
      } else {
        callback(); // 校验通过
      }
    };

    // 自定义校验：组织角色（isAutoGetRole=0时必填）
    const validateOrgRole = (rule, value, callback) => {
      const { isAutoGetRole } = this.form.getFieldsValue();
      if (isAutoGetRole === '0' && !value) {
        callback(new Error('当自动获取角色为"否"时，组织角色不能为空'));
      } else {
        callback(); // 校验通过
      }
    };

    return {
      form: this.$form.createForm(this),
      rules: {
        isAutoGetRole: [
          { required: true, message: '请选择是否自动获取角色', trigger: 'change' }
        ],
        roleOrigin: [
          { validator: validateRoleOrigin, trigger: 'blur,change' }
        ],
        orgRole: [
          { validator: validateOrgRole, trigger: 'blur,change' } // 此处修正：应为 validateOrgRole
        ]
      }
    };
  },
  methods: {
    handleSubmit(e) {
      e.preventDefault();
      this.form.validateFields((err, values) => {
        if (!err) {
          console.log('表单数据：', values);
          // 提交逻辑
        }
      });
    }
  }
};
</script>
