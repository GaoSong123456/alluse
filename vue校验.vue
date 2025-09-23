<template>
  <div class="optimized-form-container">
    <a-form-model
      ref="form"
      v-model="formData"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 16 }"
    >
      <!-- 第一组：type=1 时显示（包含输入框和下拉框） -->
      <template v-if="routeType === '1'">
        <!-- 文本输入框 -->
        <a-form-model-item
          label="名称1"
          prop="name1"
          :rules="getInputRules('name1', '名称1')"
        >
          <a-input v-model="formData.name1" placeholder="请输入名称1" />
        </a-form-model-item>
        
        <!-- 下拉框 -->
        <a-form-model-item
          label="类别11"
          prop="name11"
          :rules="getSelectRules('name11', '类别11')"
        >
          <a-select 
            v-model="formData.name11" 
            placeholder="请选择类别11"
            style="width: 100%"
          >
            <a-select-option value="opt1">选项1</a-select-option>
            <a-select-option value="opt2">选项2</a-select-option>
          </a-select>
        </a-form-model-item>
        
        <!-- 多行文本框 -->
        <a-form-model-item
          label="详情111"
          prop="name111"
          :rules="getInputRules('name111', '详情111')"
        >
          <a-textarea v-model="formData.name111" placeholder="请输入详情111" />
        </a-form-model-item>
      </template>

      <!-- 第二组：type=2 时显示（包含输入框和下拉框） -->
      <template v-if="routeType === '2'">
        <!-- 文本输入框 -->
        <a-form-model-item
          label="名称2"
          prop="name2"
          :rules="getInputRules('name2', '名称2')"
        >
          <a-input v-model="formData.name2" placeholder="请输入名称2" />
        </a-form-model-item>
        
        <!-- 下拉框 -->
        <a-form-model-item
          label="类别22"
          prop="name22"
          :rules="getSelectRules('name22', '类别22')"
        >
          <a-select 
            v-model="formData.name22" 
            placeholder="请选择类别22"
            style="width: 100%"
          >
            <a-select-option value="optA">选项A</a-select-option>
            <a-select-option value="optB">选项B</a-select-option>
          </a-select>
        </a-form-model-item>
        
        <!-- 多行文本框 -->
        <a-form-model-item
          label="详情222"
          prop="name222"
          :rules="getInputRules('name222', '详情222')"
        >
          <a-textarea v-model="formData.name222" placeholder="请输入详情222" />
        </a-form-model-item>
      </template>

      <a-form-model-item :wrapper-col="{ offset: 6, span: 16 }">
        <a-button type="primary" @click="handleButton1Click">按钮1（校验第一组）</a-button>
        <a-button style="margin-left: 10px" @click="handleButton2Click">按钮2（校验第二组）</a-button>
      </a-form-model-item>
    </a-form-model>
  </div>
</template>

<script>
import { FormModel, Input, Select, TextArea, Button, Message } from 'ant-design-vue';

const { Option: ASelectOption } = Select;

export default {
  components: {
    AFormModel: FormModel,
    AFormModelItem: FormModel.Item,
    AInput: Input,
    ASelect: Select,
    ASelectOption,
    ATextarea: TextArea,
    AButton: Button
  },
  data() {
    return {
      formData: {
        // 第一组字段
        name1: '',
        name11: '', // 下拉框值
        name111: '',
        // 第二组字段
        name2: '',
        name22: '', // 下拉框值
        name222: ''
      },
      routeType: '',
      // 分组字段集合
      group1Fields: ['name1', 'name11', 'name111'],
      group2Fields: ['name2', 'name22', 'name222']
    };
  },
  mounted() {
    this.routeType = this.$route.query.type || '';
  },
  methods: {
    /**
     * 生成输入框（含文本框、多行文本）的校验规则
     * @param {string} prop 字段prop名
     * @param {string} label 字段显示名称（用于提示）
     * @returns {Array} 校验规则
     */
    getInputRules(prop, label) {
      return [
        {
          required: true,
          message: `请输入${label}`, // 具体字段提示
          trigger: 'blur' // 输入框失焦时触发
        }
      ];
    },

    /**
     * 生成下拉框的校验规则
     * @param {string} prop 字段prop名
     * @param {string} label 字段显示名称（用于提示）
     * @returns {Array} 校验规则
     */
    getSelectRules(prop, label) {
      return [
        {
          required: true,
          message: `请选择${label}`, // 下拉框专属提示
          trigger: 'change' // 下拉框选择变化时触发
        },
        {
          // 排除空字符串或undefined（下拉框默认值可能为''）
          validator: (rule, value, callback) => {
            if (value === '' || value === undefined) {
              callback(new Error(`请选择${label}`));
            } else {
              callback();
            }
          },
          trigger: 'change'
        }
      ];
    },

    /**
     * 按钮1：校验第一组
     */
    handleButton1Click() {
      if (this.routeType !== '1') {
        Message.warning('当前模式不支持按钮1操作');
        return;
      }

      this.$refs.form.validateFields(this.group1Fields, (err) => {
        if (!err) {
          Message.success('第一组校验通过，执行操作1');
          // 业务逻辑...
        }
      });
    },

    /**
     * 按钮2：校验第二组
     */
    handleButton2Click() {
      if (this.routeType !== '2') {
        Message.warning('当前模式不支持按钮2操作');
        return;
      }

      this.$refs.form.validateFields(this.group2Fields, (err) => {
        if (!err) {
          Message.success('第二组校验通过，执行操作2');
          // 业务逻辑...
        }
      });
    }
  }
};
</script>

<style scoped>
.optimized-form-container {
  max-width: 600px;
  margin: 20px auto;
  padding: 20px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
}
</style>
