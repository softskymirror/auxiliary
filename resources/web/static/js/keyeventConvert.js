/**
 * AndroidControl Key Event Converter
 * PC 键盘按键码 → Android KeyEvent 码转换
 * 
 * 映射规则参考 Android KeyEvent 常量:
 * https://developer.android.com/reference/android/view/KeyEvent
 */

;(function() {
    'use strict';

    // PC keyCode → Android keyCode 映射表
    const KEY_MAP = {
        // 功能键
        8:   67,   // Backspace → DEL
        13:  66,   // Enter → ENTER
        32:  62,   // Space → SPACE
        16:  59,   // Shift → SHIFT_LEFT

        // 方向键
        37:  21,   // ← → DPAD_LEFT
        38:  19,   // ↑ → DPAD_UP
        39:  22,   // → → DPAD_RIGHT
        40:  20,   // ↓ → DPAD_DOWN

        // 数字键 主键盘 0-9
        48:  7,    // 0 → KEYCODE_0
        49:  8,    // 1 → KEYCODE_1
        50:  9,    // 2 → KEYCODE_2
        51:  10,   // 3 → KEYCODE_3
        52:  11,   // 4 → KEYCODE_4
        53:  12,   // 5 → KEYCODE_5
        54:  13,   // 6 → KEYCODE_6
        55:  14,   // 7 → KEYCODE_7
        56:  15,   // 8 → KEYCODE_8
        57:  16,   // 9 → KEYCODE_9

        // 数字键 小键盘 0-9
        96:  7,    // Numpad 0 → KEYCODE_0
        97:  8,    // Numpad 1 → KEYCODE_1
        98:  9,    // Numpad 2 → KEYCODE_2
        99:  10,   // Numpad 3 → KEYCODE_3
        100: 11,   // Numpad 4 → KEYCODE_4
        101: 12,   // Numpad 5 → KEYCODE_5
        102: 13,   // Numpad 6 → KEYCODE_6
        103: 14,   // Numpad 7 → KEYCODE_7
        104: 15,   // Numpad 8 → KEYCODE_8
        105: 16,   // Numpad 9 → KEYCODE_9

        // 字母键 A-Z
        65:  29,   // A → KEYCODE_A
        66:  30,   // B → KEYCODE_B
        67:  31,   // C → KEYCODE_C
        68:  32,   // D → KEYCODE_D
        69:  33,   // E → KEYCODE_E
        70:  34,   // F → KEYCODE_F
        71:  35,   // G → KEYCODE_G
        72:  36,   // H → KEYCODE_H
        73:  37,   // I → KEYCODE_I
        74:  38,   // J → KEYCODE_J
        75:  39,   // K → KEYCODE_K
        76:  40,   // L → KEYCODE_L
        77:  41,   // M → KEYCODE_M
        78:  42,   // N → KEYCODE_N
        79:  43,   // O → KEYCODE_O
        80:  44,   // P → KEYCODE_P
        81:  45,   // Q → KEYCODE_Q
        82:  46,   // R → KEYCODE_R
        83:  47,   // S → KEYCODE_S
        84:  48,   // T → KEYCODE_T
        85:  49,   // U → KEYCODE_U
        86:  50,   // V → KEYCODE_V
        87:  51,   // W → KEYCODE_W
        88:  52,   // X → KEYCODE_X
        89:  53,   // Y → KEYCODE_Y
        90:  54    // Z → KEYCODE_Z
    };

    /**
     * 将 PC 键盘 keyCode 转换为 Android KeyEvent keyCode
     * @param {number} keyCode - PC 键盘事件 keyCode
     * @returns {number} Android KeyEvent 码
     */
    window.convertAndroidKeyCode = function(keyCode) {
        return KEY_MAP[keyCode] !== undefined ? KEY_MAP[keyCode] : keyCode;
    };

})();
