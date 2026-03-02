<template>
  <Transition name="fade">
    <div v-if="uiStore.loadingActive" class="loading-overlay">
      <div class="loading-box">
        <div class="pot-scene">
          <div class="steam steam-1"></div>
          <div class="steam steam-2"></div>
          <div class="steam steam-3"></div>
          <div class="lid"></div>
          <div class="pot">
            <div class="pot-shine"></div>
          </div>
          <div class="pot-handle pot-handle--left"></div>
          <div class="pot-handle pot-handle--right"></div>
        </div>
        <p v-if="uiStore.loadingMessage" class="loading-message">{{ uiStore.loadingMessage }}</p>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { useUiStore } from '@/stores/uiStore'

const uiStore = useUiStore()
</script>

<style scoped>
.loading-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.loading-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.loading-message {
  color: #fff;
  font-size: 1rem;
  letter-spacing: 0.02em;
}

/* --- Pot scene --- */
.pot-scene {
  position: relative;
  width: 80px;
  height: 90px;
}

/* Steam */
.steam {
  position: absolute;
  bottom: 72px;
  width: 8px;
  height: 20px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.7);
  animation: steam-rise 1.4s ease-in-out infinite;
  opacity: 0;
}

.steam-1 { left: 22px; animation-delay: 0s; }
.steam-2 { left: 36px; animation-delay: 0.3s; }
.steam-3 { left: 50px; animation-delay: 0.6s; }

@keyframes steam-rise {
  0%   { opacity: 0; transform: translateY(0) scaleX(1); }
  30%  { opacity: 1; }
  100% { opacity: 0; transform: translateY(-22px) scaleX(1.6); }
}

/* Lid */
.lid {
  position: absolute;
  bottom: 52px;
  left: 50%;
  transform: translateX(-50%);
  width: 66px;
  height: 12px;
  background: #a0aec0;
  border-radius: 6px 6px 2px 2px;
  animation: lid-bounce 1.4s ease-in-out infinite;
}

.lid::after {
  content: '';
  position: absolute;
  top: -6px;
  left: 50%;
  transform: translateX(-50%);
  width: 10px;
  height: 8px;
  background: #718096;
  border-radius: 4px 4px 0 0;
}

@keyframes lid-bounce {
  0%, 100% { transform: translateX(-50%) translateY(0); }
  40%       { transform: translateX(-50%) translateY(-6px); }
  60%       { transform: translateX(-50%) translateY(-3px); }
}

/* Pot body */
.pot {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 64px;
  height: 52px;
  background: #718096;
  border-radius: 4px 4px 14px 14px;
  overflow: hidden;
}

.pot-shine {
  position: absolute;
  top: 6px;
  left: 8px;
  width: 16px;
  height: 28px;
  background: rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  transform: rotate(-10deg);
}

/* Handles */
.pot-handle {
  position: absolute;
  bottom: 18px;
  width: 10px;
  height: 20px;
  background: #4a5568;
  border-radius: 4px;
}

.pot-handle--left  { left: 3px; transform: rotate(-15deg); }
.pot-handle--right { right: 3px; transform: rotate(15deg); }

/* Fade transition */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
