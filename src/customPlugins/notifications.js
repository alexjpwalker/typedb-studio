import Vue from 'vue';
import Toasted from 'vue-toasted';
import logger from '@/logger';

Vue.use(Toasted);

const SUCCESS_DURATION = 4000;
const INFO_DURATION = 5000;

function registerNotifications() {
  Vue.prototype.$notifySuccess = function successFn(message) {
    this.$toasted.clear();
    this.$toasted.show(message, {
      action: {
        text: 'CLOSE',
        onClick: (e, toastObject) => {
          toastObject.goAway(0);
        },
      },
      duration: SUCCESS_DURATION,
      position: 'bottom-right',
    });
  };

  Vue.prototype.$notifyInfo = function infoFn(message) {
    this.$toasted.clear();
    this.$toasted.show(message, {
      action: {
        text: 'CLOSE',
        onClick: (e, toastObject) => {
          toastObject.goAway(0);
        },
      },
      duration: INFO_DURATION,
      position: 'bottom-right',
      type: 'info',
    });
  };

  Vue.prototype.$notifyConfirmDelete = function confirmFn(message, confirmCb) {
    this.$toasted.clear();
    this.$toasted.show(message, {
      action: [
        {
          text: 'Cancel',
          onClick: (e, toastObject) => {
            toastObject.goAway(0);
          },
        },
        {
          text: 'Confirm',
          onClick: (e, toastObject) => {
            confirmCb();
            toastObject.goAway(0);
          },
          class: 'confirm',
        },
      ],
      position: 'bottom-right',
    });
  };

  Vue.prototype.$notifyError = function errorFn(e, operation) {
    let errorMessage = e;
    logger.error(e);
    if (operation) errorMessage = `${errorMessage}<br><br>Action: [${operation}]`;

    this.$toasted.show(errorMessage, {
      action: {
        text: 'CLOSE',
        onClick: (e, toastObject) => {
          toastObject.goAway(0);
        },
      },
      position: 'bottom-right',
      type: 'error',
      className: 'notify-error',
    });
  };
}


export default { registerNotifications };
