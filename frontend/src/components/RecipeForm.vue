<template>
  <form class="recipe-form" @submit.prevent="handleSubmit">
    <div class="form-group">
      <label for="title">Titel</label>
      <input
        id="title"
        v-model="formData.title"
        type="text"
        required
        placeholder="Rezeptname eingeben"
      />
    </div>

    <div class="form-group">
      <label for="description">Beschreibung</label>
      <textarea
        id="description"
        v-model="formData.description"
        rows="3"
        placeholder="Kurze Beschreibung eingeben"
      ></textarea>
    </div>

    <div class="form-group">
      <label>Bild</label>
      <div class="image-upload">
        <input
          type="file"
          accept="image/*"
          @change="handleImageUpload"
          id="image-upload"
        />
        <div v-if="formData.imageUrl" class="image-preview">
          <img :src="formData.imageUrl" alt="Rezept-Bild" />
          <button type="button" class="btn-remove-image" @click="removeImage">
            Bild entfernen
          </button>
        </div>
      </div>
    </div>

    <div class="form-group">
      <label for="servings">Personenanzahl</label>
      <input
        id="servings"
        v-model.number="formData.baseServings"
        type="number"
        min="1"
        max="100"
        required
      />
    </div>

    <div class="form-group">
      <label>Quelle</label>
      <div class="source-fields">
        <input
          v-model="formData.author"
          type="text"
          placeholder="Autor"
        />
        <input
          v-model="formData.source"
          type="text"
          placeholder="Buch oder Website"
        />
        <input
          v-model="formData.page"
          type="text"
          placeholder="Seite (bei Büchern)"
        />
      </div>
    </div>

    <div class="form-group">
      <label>Zutaten</label>
      <div v-for="(ingredient, index) in formData.ingredients" :key="index" class="ingredient-row">
        <input
          v-model="ingredient.name"
          type="text"
          placeholder="Zutat"
          required
        />
        <input
          v-model="ingredient.amount"
          type="text"
          placeholder="Menge"
          required
        />
        <input
          v-model="ingredient.unit"
          type="text"
          placeholder="Einheit"
        />
        <button type="button" class="btn-remove" @click="removeIngredient(index)">
          Entfernen
        </button>
      </div>
      <button type="button" class="btn-add" @click="addIngredient">
        + Zutat hinzufügen
      </button>
    </div>

    <div class="form-group">
      <label>Arbeitsanweisungen</label>
      <div v-for="(instruction, index) in formData.instructions" :key="index" class="instruction-row">
        <span class="step-number">{{ index + 1 }}.</span>
        <textarea
          v-model="formData.instructions[index]"
          rows="2"
          placeholder="Arbeitsschritt eingeben"
          required
        ></textarea>
        <button type="button" class="btn-remove" @click="removeInstruction(index)">
          Entfernen
        </button>
      </div>
      <button type="button" class="btn-add" @click="addInstruction">
        + Schritt hinzufügen
      </button>
    </div>

    <div class="form-actions">
      <button type="button" class="btn-cancel" @click="handleCancel">
        Abbrechen
      </button>
      <button type="submit" class="btn-submit">
        {{ isEdit ? 'Rezept aktualisieren' : 'Rezept erstellen' }}
      </button>
    </div>
  </form>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  recipe: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['submit', 'cancel'])

const router = useRouter()
const isEdit = ref(!!props.recipe)

const formData = ref({
  title: '',
  description: '',
  baseServings: 4,
  imageUrl: '',
  author: '',
  source: '',
  page: '',
  ingredients: [{ name: '', amount: '', unit: '' }],
  instructions: ['']
})

watch(
  () => props.recipe,
  (newRecipe) => {
    if (newRecipe) {
      isEdit.value = true
      formData.value = {
        title: newRecipe.title || '',
        description: newRecipe.description || '',
        baseServings: newRecipe.baseServings || 4,
        imageUrl: newRecipe.imageUrl || '',
        author: newRecipe.author || '',
        source: newRecipe.source || '',
        page: newRecipe.page || '',
        ingredients: newRecipe.ingredients?.length
          ? [...newRecipe.ingredients]
          : [{ name: '', amount: '', unit: '' }],
        instructions: newRecipe.instructions?.length
          ? [...newRecipe.instructions]
          : ['']
      }
    }
  },
  { immediate: true }
)

const addIngredient = () => {
  formData.value.ingredients.push({ name: '', amount: '', unit: '' })
}

const handleImageUpload = (event) => {
  const file = event.target.files[0]
  if (file) {
    const reader = new FileReader()
    reader.onload = (e) => {
      formData.value.imageUrl = e.target.result
    }
    reader.readAsDataURL(file)
  }
}

const removeImage = () => {
  formData.value.imageUrl = ''
}

const removeIngredient = (index) => {
  if (formData.value.ingredients.length > 1) {
    formData.value.ingredients.splice(index, 1)
  }
}

const addInstruction = () => {
  formData.value.instructions.push('')
}

const removeInstruction = (index) => {
  if (formData.value.instructions.length > 1) {
    formData.value.instructions.splice(index, 1)
  }
}

const handleSubmit = () => {
  const recipeData = {
    ...formData.value,
    ingredients: formData.value.ingredients.filter((i) => i.name.trim()),
    instructions: formData.value.instructions.filter((i) => i.trim())
  }
  emit('submit', recipeData)
}

const handleCancel = () => {
  router.push('/')
}
</script>

<style scoped>
.recipe-form {
  max-width: 600px;
  margin: 0 auto;
}

.form-group {
  margin-bottom: 24px;
}

.form-group label {
  display: block;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--color-text-primary, #333);
}

.form-group input[type="text"],
.form-group input[type="number"],
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  font-size: 1rem;
  font-family: inherit;
  box-sizing: border-box;
}

.image-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.image-preview {
  position: relative;
  display: inline-block;
}

.image-preview img {
  max-width: 300px;
  max-height: 200px;
  border-radius: 8px;
  object-fit: cover;
}

.btn-remove-image {
  margin-top: 8px;
  padding: 6px 12px;
  border: 1px solid var(--color-error, #e53e3e);
  border-radius: 4px;
  background: transparent;
  color: var(--color-error, #e53e3e);
  cursor: pointer;
  font-size: 0.875rem;
}

.btn-remove-image:hover {
  background: rgba(229, 62, 62, 0.1);
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--color-primary, #4a5568);
  box-shadow: 0 0 0 3px rgba(74, 85, 104, 0.1);
}

.source-fields {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.source-fields input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border, #ddd);
  border-radius: 6px;
  font-size: 1rem;
  box-sizing: border-box;
}

.ingredient-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.ingredient-row input {
  flex: 1;
}

.ingredient-row input:first-child {
  flex: 2;
}

.instruction-row {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: flex-start;
}

.step-number {
  font-weight: 600;
  color: var(--color-text-muted, #999);
  min-width: 24px;
  padding-top: 10px;
}

.instruction-row textarea {
  flex: 1;
}

.btn-add,
.btn-remove {
  padding: 8px 16px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.875rem;
  transition: background-color 0.2s ease;
}

.btn-add {
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
}

.btn-add:hover {
  background: var(--color-border, #ddd);
}

.btn-remove {
  background: transparent;
  color: var(--color-error, #e53e3e);
}

.btn-remove:hover {
  background: rgba(229, 62, 62, 0.1);
}

.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 32px;
}

.btn-cancel,
.btn-submit {
  padding: 12px 24px;
  border: none;
  border-radius: 6px;
  font-size: 1rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.btn-cancel {
  background: var(--color-bg-secondary, #f0f0f0);
  color: var(--color-text-primary, #333);
}

.btn-cancel:hover {
  background: var(--color-border, #ddd);
}

.btn-submit {
  background: var(--color-primary, #4a5568);
  color: white;
}

.btn-submit:hover {
  background: var(--color-primary-dark, #2d3748);
}
</style>
