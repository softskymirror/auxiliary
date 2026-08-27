/**
 * AndroidControl Device Controller
 * 设备控制页面核心逻辑
 * 
 * 功能：
 * - WebSocket 实时通信（TextProtocol + BinaryProtocol）
 * - 屏幕镜像显示（Minicap JPEG 流）
 * - 触摸/按键注入（Minitouch 协议）
 * - 设备管理控制（安装/重启/截屏/CLI 等）
 * - 连接状态管理（自动重连/错误提示）
 */

;(function() {
    'use strict';

    // ========== 常量定义 ==========
    const NAV_HEIGHT = 24;
    const FOOTER_HEIGHT = 42;
    const DEFAULT_IP = window.location.hostname || '127.0.0.1';
    const DEFAULT_PORT = parseInt(window.location.port) || 6655;
    const RECONNECT_DELAY = 3000;

    // ========== 应用状态 ==========
    const state = {
        deviceInfo: null,
        deviceWindow: null,
        net: null,
        canvas: null,
        ctx: null,
        isDown: false,
        lastObjectURL: null,
        capReady: false,
        eventReady: false,
        reconnectTimer: null
    };

    // ========== 协议解析器 ==========
    class Protocol {
        static parse(text) {
            const idx = text.indexOf('://');
            if (idx === -1) return null;
            return {
                header: text.substring(0, idx),
                body: text.substring(idx + 3)
            };
        }

        static build(header, body) {
            return header + '://' + (body || '{}');
        }
    }

    // ========== 网络管理器 ==========
    class NetworkManager {
        constructor(ip, port) {
            this.ip = ip;
            this.port = port;
            this.ws = null;
            this.handlers = {};
            this.connected = false;
        }

        connect(handlers) {
            this.handlers = handlers;
            try {
                this.ws = new WebSocket('ws://' + this.ip + ':' + this.port);
            } catch (err) {
                console.error('WebSocket 创建失败:', err);
                this.showError('连接失败');
                return;
            }

            this.ws.onopen = () => {
                this.connected = true;
                this.updateStatus('connected');
                if (this.handlers.onopen) this.handlers.onopen();
            };

            this.ws.onclose = () => {
                this.connected = false;
                this.updateStatus('disconnected');
                if (this.handlers.onclose) this.handlers.onclose();
            };

            this.ws.onerror = (err) => {
                console.error('WebSocket 错误:', err);
                this.updateStatus('error');
            };

            this.ws.onmessage = (event) => {
                const data = event.data;
                if (typeof data === 'string') {
                    this.handleText(data);
                } else {
                    this.handleBinary(data);
                }
            };
        }

        send(header, body) {
            if (!this.connected || !this.ws) {
                console.warn('WebSocket 未连接');
                return;
            }
            const msg = body !== undefined ? Protocol.build(header, JSON.stringify(body)) : header;
            this.ws.send(msg);
        }

        sendRaw(str) {
            if (!this.connected || !this.ws) {
                console.warn('WebSocket 未连接');
                return;
            }
            this.ws.send(str);
        }

        handleText(text) {
            const proto = Protocol.parse(text);
            if (!proto) {
                console.warn('无效协议格式:', text);
                return;
            }
            const handler = this.handlers[proto.header];
            if (handler) {
                handler.call(this.handlers, proto.body);
            } else {
                console.warn('未处理的协议:', proto.header);
            }
        }

        handleBinary(data) {
            const reader = new FileReader();
            reader.onload = () => {
                const headerType = new Int16Array(reader.result)[0];
                if (headerType === 0x0011 && this.handlers.SM_JPG) {
                    this.handlers.SM_JPG(data.slice(6));
                }
            };
            reader.readAsArrayBuffer(data.slice(0, 2));
        }

        updateStatus(status) {
            const indicator = document.getElementById('connection-status');
            const text = document.getElementById('connection-text');
            if (!indicator || !text) return;

            const statusMap = {
                'connected': { color: '#4CAF50', text: '已连接' },
                'disconnected': { color: '#f44336', text: '已断开' },
                'connecting': { color: '#ff9800', text: '连接中...' },
                'error': { color: '#f44336', text: '连接错误' }
            };

            const info = statusMap[status] || statusMap['disconnected'];
            indicator.style.backgroundColor = info.color;
            text.textContent = info.text;
        }

        showError(msg) {
            console.error(msg);
            const text = document.getElementById('connection-text');
            if (text) text.textContent = msg;
        }

        close() {
            if (this.ws) {
                this.ws.close();
                this.ws = null;
            }
            if (state.reconnectTimer) {
                clearTimeout(state.reconnectTimer);
                state.reconnectTimer = null;
            }
        }
    }

    // ========== 设备信息 ==========
    class DeviceInfo {
        constructor(sn, w, h) {
            this.serialNumber = sn;
            this.physicsSize = { w: parseInt(w) || 1080, h: parseInt(h) || 1920 };
        }
    }

    // ========== 设备窗口管理 ==========
    class DeviceWindow {
        constructor(deviceInfo) {
            this.deviceInfo = deviceInfo;
            this.scale = 0.3;
            this.rotate = false;
            this.keyMap = false;
            this.displaySize = {
                w: deviceInfo.physicsSize.w * this.scale,
                h: deviceInfo.physicsSize.h * this.scale
            };
        }

        resize() {
            const w = this.displaySize.w;
            const h = this.displaySize.h + NAV_HEIGHT + FOOTER_HEIGHT;
            const content = document.getElementById('content');
            if (content) {
                content.style.width = w + 'px';
                content.style.height = h + 'px';
            }

            if (state.canvas) {
                state.canvas.width = this.displaySize.w;
                state.canvas.height = this.displaySize.h;
            }
        }

        setDisplayScale(scale) {
            this.displaySize.w = parseInt(this.deviceInfo.physicsSize.w * scale);
            this.displaySize.h = parseInt(this.deviceInfo.physicsSize.h * scale);
            this.resize();
        }
    }

    // ========== 触摸事件处理 ==========
    const TouchHandler = {
        sendTouchEvent(cmd) {
            if (state.net) state.net.sendRaw('M_TOUCH://' + cmd);
        },

        sendKeyEvent(code) {
            if (state.net) state.net.sendRaw('M_KEYEVENT://' + code);
        },

        transformCoords(argx, argy, isRotate) {
            const scaleX = state.deviceInfo.physicsSize.w / state.canvas.width;
            const scaleY = state.deviceInfo.physicsSize.h / state.canvas.height;
            let x = argx, y = argy;

            if (isRotate) {
                x = (state.canvas.height - argy) * (state.canvas.width / state.canvas.height);
                y = argx * (state.canvas.height / state.canvas.width);
            }

            return {
                x: Math.round(x * scaleX),
                y: Math.round(y * scaleY)
            };
        },

        sendDown(argx, argy) {
            const coords = this.transformCoords(argx, argy, state.deviceWindow.rotate);
            this.sendTouchEvent('d 0 ' + coords.x + ' ' + coords.y + ' 50\nc\n');
        },

        sendMove(argx, argy) {
            const coords = this.transformCoords(argx, argy, state.deviceWindow.rotate);
            this.sendTouchEvent('m 0 ' + coords.x + ' ' + coords.y + ' 50\nc\n');
        },

        sendUp() {
            this.sendTouchEvent('u 0\nc\n');
        },

        getRelativePos(event) {
            const rect = state.canvas.getBoundingClientRect();
            return {
                x: event.clientX - rect.left,
                y: event.clientY - rect.top
            };
        },

        init() {
            const canvas = state.canvas;

            canvas.onmousedown = (event) => {
                state.isDown = true;
                const pos = this.getRelativePos(event);
                this.sendDown(pos.x, pos.y);
            };

            canvas.onmousemove = (event) => {
                if (!state.isDown) return;
                const pos = this.getRelativePos(event);
                this.sendMove(pos.x, pos.y);
            };

            canvas.onmouseup = () => {
                if (!state.isDown) return;
                state.isDown = false;
                this.sendUp();
            };

            canvas.onmouseout = () => {
                if (!state.isDown) return;
                state.isDown = false;
                this.sendUp();
            };

            // 阻止触摸滚动
            canvas.addEventListener('touchstart', (e) => e.preventDefault(), false);
            canvas.addEventListener('touchmove', (e) => e.preventDefault(), false);
        }
    };

    // ========== 图像渲染 ==========
    const ImageRenderer = {
        renderJPG(jpgData) {
            // 释放上一个 Object URL 防止内存泄漏
            if (state.lastObjectURL) {
                URL.revokeObjectURL(state.lastObjectURL);
            }

            const blob = new Blob([jpgData], { type: 'image/jpeg' });
            const url = URL.createObjectURL(blob);
            state.lastObjectURL = url;

            const img = new Image();
            img.onload = () => {
                if (state.canvas && state.ctx) {
                    state.ctx.drawImage(img, 0, 0, state.canvas.width, state.canvas.height);
                    state.canvas.img = img;
                }
                img.onload = null;
            };
            img.src = url;

            // 请求下一帧
            if (state.net) state.net.send('M_WAITTING', null);
        },

        takeScreenshot() {
            if (!state.canvas) return;
            state.canvas.toBlob((blob) => {
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'screenshot_' + state.deviceInfo.serialNumber + '_' + Date.now() + '.jpg';
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                setTimeout(() => URL.revokeObjectURL(url), 100);
            }, 'image/jpeg', 0.95);
        }
    };

    // ========== 设备管理功能 ==========
    const DeviceManager = {
        setConstant() {
            if (state.net) state.net.send('M_START', { type: 'constant' });
            alert('已设置常量状态（屏幕常亮）');
        },

        turnOffMobileData() {
            if (state.net) state.net.send('M_START', { type: 'turnoff' });
            alert('已关闭移动数据');
        },

        makeSound() {
            if (state.net) state.net.send('M_START', { type: 'ring' });
        },

        resetSystem() {
            if (confirm('确认重启设备至 Recovery 模式？')) {
                if (state.net) state.net.send('M_START', { type: 'reset' });
            }
        },

        installApps() {
            const dirInput = document.getElementById('group-dir-input');
            const codeInput = document.getElementById('device-code-input');
            const addToolCheckbox = document.querySelector('input[name="install-tool"]');

            const dir = dirInput ? dirInput.value : '';
            const code = codeInput ? codeInput.value : '';

            if (!dir || !code) {
                alert('请在参数设置中填写项目地址和设备编号');
                return;
            }

            const addTool = addToolCheckbox ? addToolCheckbox.checked : false;
            if (state.net) {
                state.net.send('M_START', {
                    type: 'install',
                    config: { dir: dir, code: code, addTool: addTool }
                });
            }
            alert('正在安装，请勿操作设备...');
        },

        executeCLI() {
            const cmdInput = document.getElementById('cli-cmd-input');
            const portInput = document.getElementById('server-port-input');

            const cmd = cmdInput ? cmdInput.value : '';
            const port = portInput ? portInput.value : '';

            if (!cmd) {
                alert('请输入命令');
                return;
            }

            if (state.net) {
                state.net.send('M_START', {
                    type: 'callCLi',
                    config: { cmd: cmd, port: port }
                });
            }
        },

        startCapService() {
            if (!state.net || !state.deviceWindow) return;
            state.net.send('M_START', {
                type: 'cap',
                config: {
                    rotate: state.deviceWindow.rotate ? 90 : 0,
                    scale: state.deviceWindow.scale
                }
            });
        },

        startEventService() {
            if (state.net) state.net.send('M_START', { type: 'event' });
        }
    };

    // ========== UI 控制器 ==========
    const UIController = {
        // 创建输入框
        createInputs() {
            const containers = [
                { id: 'group-dir', inputId: 'group-dir-input', placeholder: '例如: D:\\apps\\' },
                { id: 'device-code', inputId: 'device-code-input', placeholder: '设备编号' },
                { id: 'server-port', inputId: 'server-port-input', placeholder: '端口号' },
                { id: 'cli-cmd', inputId: 'cli-cmd-input', placeholder: 'Shell 命令' }
            ];

            containers.forEach(cfg => {
                const container = document.getElementById(cfg.id);
                if (!container) return;
                const input = document.createElement('input');
                input.id = cfg.inputId;
                input.type = 'text';
                input.placeholder = cfg.placeholder;
                input.style.width = '100%';
                input.style.padding = '5px';
                container.appendChild(input);
            });

            // 输入验证
            const dirInput = document.getElementById('group-dir-input');
            if (dirInput) {
                dirInput.addEventListener('blur', (e) => {
                    if (e.target.value && !this.isValidPath(e.target.value)) {
                        alert('路径格式无效，应为: X:\\path\\');
                        e.target.value = '';
                    }
                });
            }
        },

        isValidPath(path) {
            return /^[A-Za-z]:\\(.+\\)*$/.test(path);
        },

        // 初始化 Tab 切换
        initTabs() {
            const tabs = [
                { id: 'group', page: 'make-task' },
                { id: 'farewell', page: 'search-info' },
                { id: 'operation', page: 'run-operation' },
                { id: 'settings', page: 'value-settings' }
            ];

            tabs.forEach(tab => {
                const btn = document.getElementById(tab.id);
                if (!btn) return;

                btn.addEventListener('click', () => {
                    // 隐藏所有页面
                    tabs.forEach(t => {
                        const page = document.getElementById(t.page);
                        const btn = document.getElementById(t.id);
                        if (page) page.style.display = 'none';
                        if (btn) btn.style.backgroundColor = '#9d9d9d';
                    });

                    // 显示当前页面
                    const currentPage = document.getElementById(tab.page);
                    if (currentPage) currentPage.style.display = 'block';
                    btn.style.backgroundColor = '#FFFFFF';
                });
            });
        },

        // 初始化滑块
        initSliders() {
            // 显示缩放
            $('#display-scale-slider').slider({
                max: 100, min: 10, step: 5, value: 20,
                change: () => {
                    const scale = $('#display-scale-slider').slider('value') / 100;
                    state.deviceWindow.setDisplayScale(scale);
                    if (window.__titleVM) {
                        window.__titleVM.displaySize = { ...state.deviceWindow.displaySize };
                    }
                    if (state.canvas && state.canvas.img) {
                        state.ctx.drawImage(state.canvas.img, 0, 0, state.canvas.width, state.canvas.height);
                    }
                }
            });

            // 输出清晰度
            $('#scale-slider').slider({
                max: 100, min: 5, step: 5, value: 30,
                change: () => {
                    state.deviceWindow.scale = $('#scale-slider').slider('value') / 100;
                    if (window.__titleVM) {
                        window.__titleVM.outputScale = state.deviceWindow.scale;
                    }
                    DeviceManager.startCapService();
                    $('#myModal').modal('hide');
                    $('#resetScaleModal').modal('show');
                }
            });
        },

        // 初始化按钮事件
        initButtons() {
            // 导航键
            $('#btn-back').on('click', () => TouchHandler.sendKeyEvent(4));
            $('#btn-home').on('click', () => TouchHandler.sendKeyEvent(3));
            $('#btn-menu').on('click', () => TouchHandler.sendKeyEvent(82));

            // 更多设置
            $('#btn-more').on('click', () => {
                // Bootstrap modal 自动处理
            });

            // 设备管理
            $('#set-constant').on('click', () => DeviceManager.setConstant());
            $('#turnoff-mobiledata').on('click', () => DeviceManager.turnOffMobileData());
            $('#shot-screen').on('click', () => ImageRenderer.takeScreenshot());
            $('#make-sound').on('click', () => DeviceManager.makeSound());
            $('#install-app').on('click', () => DeviceManager.installApps());
            $('#run-cmd').on('click', () => DeviceManager.executeCLI());
            $('#reset-system').on('click', () => DeviceManager.resetSystem());

            // 底部快捷按钮
            $('#display-no-sleep').on('click', () => DeviceManager.setConstant());
            $('#close-data').on('click', () => DeviceManager.turnOffMobileData());

            // 屏幕旋转
            $('#rotateCheckBox').on('click', () => {
                state.deviceWindow.rotate = $('#rotateCheckBox').prop('checked');
                DeviceManager.startCapService();
                $('#myModal').modal('hide');
                $('#resetScaleModal').modal('show');
            });

            // 键盘映射
            $('#keyEventCheckBox').on('click', () => {
                state.deviceWindow.keyMap = $('#keyEventCheckBox').prop('checked');
            });
        },

        // 初始化键盘事件
        initKeyboard() {
            $(document).keypress((event) => {
                if (state.deviceWindow && state.deviceWindow.keyMap) {
                    const keyCode = event.keyCode;
                    const androidKey = convertAndroidKeyCode(keyCode);
                    TouchHandler.sendKeyEvent(androidKey);
                }
            });
        },

        // 初始化窗口大小调整
        initResize() {
            window.addEventListener('resize', () => {
                if (state.canvas && state.canvas.img) {
                    state.ctx.drawImage(state.canvas.img, 0, 0, state.canvas.width, state.canvas.height);
                }
            });
        }
    };

    // ========== URL 参数解析 ==========
    function parseUrlParams() {
        const params = {};
        const search = window.location.search.substring(1);
        if (!search) return params;

        search.split('&').forEach(pair => {
            const [key, value] = pair.split('=');
            params[key] = decodeURIComponent(value);
        });
        return params;
    }

    // ========== Vue 标题组件 ==========
    let titleVM = null;
    window.__titleVM = null;

    function initVueTitle() {
        titleVM = new Vue({
            el: '#title',
            data: {
                displaySize: state.deviceWindow ? { ...state.deviceWindow.displaySize } : { w: 1080, h: 1920 },
                outputScale: state.deviceWindow ? state.deviceWindow.scale : 0.3
            },
            computed: {
                title: function() {
                    return this.displaySize.w + 'x' + this.displaySize.h +
                        '  |  ' + parseInt(state.deviceInfo.physicsSize.w * this.outputScale) +
                        'x' + parseInt(state.deviceInfo.physicsSize.h * this.outputScale);
                }
            }
        });
        window.__titleVM = titleVM;
    }

    // ========== 初始化 ==========
    function init() {
        // 解析 URL 参数
        const urlParams = parseUrlParams();
        const sn = urlParams.sn || 'unknown';
        const w = urlParams.w || 1080;
        const h = urlParams.h || 1920;

        // 初始化状态
        state.deviceInfo = new DeviceInfo(sn, w, h);
        state.canvas = document.getElementById('phone-screen');
        state.ctx = state.canvas.getContext('2d');

        // 创建输入框
        UIController.createInputs();

        // 初始化设备窗口
        state.deviceWindow = new DeviceWindow(state.deviceInfo);
        state.deviceWindow.resize();

        // 初始化 Vue 标题
        initVueTitle();

        // 初始化 UI
        UIController.initTabs();
        UIController.initSliders();
        UIController.initButtons();
        UIController.initKeyboard();
        UIController.initResize();

        // 初始化触摸处理
        TouchHandler.init();

        // 连接服务器
        connectToServer();
    }

    // ========== 连接服务器 ==========
    function connectToServer() {
        state.net = new NetworkManager(DEFAULT_IP, DEFAULT_PORT);
        state.net.connect({
            onopen() {
                console.log('已连接到服务器');
                state.net.send('M_WAIT', { sn: state.deviceInfo.serialNumber });
            },

            onclose() {
                console.log('连接已断开');
                // 3秒后尝试重连
                if (state.reconnectTimer) clearTimeout(state.reconnectTimer);
                state.reconnectTimer = setTimeout(() => {
                    console.log('尝试重新连接...');
                    connectToServer();
                }, RECONNECT_DELAY);
            },

            SM_OPENED() {
                console.log('设备绑定成功');
                DeviceManager.startCapService();
                DeviceManager.startEventService();
            },

            SM_SERVICE_STATE(body) {
                try {
                    const obj = JSON.parse(body);
                    console.log('服务状态:', obj.type, obj.stat);

                    if (obj.type === 'cap') {
                        state.capReady = (obj.stat === 'open');
                        if (state.capReady) {
                            $('#resetScaleModal').modal('hide');
                            if (state.net) state.net.send('M_WAITTING', null);
                        }
                    } else if (obj.type === 'event') {
                        state.eventReady = (obj.stat === 'open');
                    }
                } catch (err) {
                    console.error('解析服务状态失败:', err);
                }
            },

            SM_JPG(jpgData) {
                ImageRenderer.renderJPG(jpgData);
            },

            // 设备列表更新通知（设备管理页无需处理）
            SM_DEVICES(body) {},

            // 操作结果回调
            SM_RESULT(body) {
                try {
                    const obj = JSON.parse(body);
                    if (obj.message) {
                        console.log('操作结果:', obj.message);
                    }
                } catch (e) {
                    console.log('操作结果:', body);
                }
            }
        });
    }

    // ========== 页面卸载清理 ==========
    window.addEventListener('beforeunload', () => {
        if (state.net) state.net.close();
        if (state.lastObjectURL) URL.revokeObjectURL(state.lastObjectURL);
        if (state.reconnectTimer) clearTimeout(state.reconnectTimer);
    });

    // ========== 启动 ==========
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
