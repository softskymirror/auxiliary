/**
 * AndroidControl Index Page
 * 设备列表首页核心逻辑
 * 
 * 功能：
 * - WebSocket 连接后端获取设备列表
 * - 设备卡片展示（截图 + 设备信息）
 * - 点击设备打开控制页面
 * - 支持多服务器管理
 */

;(function() {
    'use strict';

    // ========== 常量 ==========
    const DEFAULT_IP = window.location.hostname || 'localhost';
    const DEFAULT_PORT = parseInt(window.location.port) || 6655;

    // ========== 协议工具 ==========
    const Protocol = {
        parse(text) {
            const idx = text.indexOf('://');
            if (idx === -1) return null;
            return { header: text.substring(0, idx), body: text.substring(idx + 3) };
        },
        build(header, body) {
            return header + '://' + (body ? JSON.stringify(body) : '{}');
        }
    };

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
                return;
            }

            this.ws.onopen = () => {
                this.connected = true;
                if (this.handlers.onopen) this.handlers.onopen();
            };

            this.ws.onclose = () => {
                this.connected = false;
                if (this.handlers.onclose) this.handlers.onclose();
            };

            this.ws.onerror = () => {
                console.error('WebSocket 连接错误');
            };

            this.ws.onmessage = (event) => {
                const data = event.data;
                if (typeof data === 'string') {
                    this.handleText(data);
                }
                // 首页不需要处理二进制数据
            };
        }

        send(header, body) {
            if (!this.connected || !this.ws) return;
            this.ws.send(Protocol.build(header, body));
        }

        handleText(text) {
            const proto = Protocol.parse(text);
            if (!proto) return;
            const handler = this.handlers[proto.header];
            if (handler) {
                handler.call(this.handlers, proto.body);
            }
        }

        close() {
            if (this.ws) {
                this.ws.close();
                this.ws = null;
            }
        }
    }

    // ========== 设备模型 ==========
    class Device {
        constructor(config, server) {
            this.w = config.w || 0;
            this.h = config.h || 0;
            this.sn = config.sn || 'unknown';
            this.server = server;
        }
    }

    // ========== 服务器模型 ==========
    class Server {
        constructor(ip, port) {
            this.ip = ip;
            this.port = port;
            this.connected = false;
            this.devices = [];
            this.net = null;
        }

        connect() {
            this.net = new NetworkManager(this.ip, this.port);
            const self = this;

            this.net.connect({
                onopen() {
                    self.connected = true;
                    self.net.send('M_DEVICES');
                    serverList.updateServerStatus(self.ip, self.port, true);
                },
                onclose() {
                    self.connected = false;
                    serverList.updateServerStatus(self.ip, self.port, false);
                },
                SM_DEVICES(body) {
                    try {
                        const devicesConf = JSON.parse(body);
                        self.devices = devicesConf.map(conf => new Device(conf, self));

                        // 更新全局设备列表
                        deviceList.clearServerDevices(self);
                        self.devices.forEach(d => deviceList.addDevice(d));
                    } catch (err) {
                        console.error('解析设备列表失败:', err);
                    }
                }
            });
        }

        disconnect() {
            if (this.net) {
                this.net.close();
                this.net = null;
            }
            this.connected = false;
        }
    }

    // ========== 设备列表 Vue 组件 ==========
    const deviceList = new Vue({
        el: '#phone-list',
        data: {
            devices: []
        },
        methods: {
            clearServerDevices(server) {
                this.devices = this.devices.filter(d => d.server !== server);
            },
            addDevice(device) {
                // 避免重复添加
                const exists = this.devices.some(d => d.sn === device.sn && d.server === device.server);
                if (!exists) {
                    this.devices.push(device);
                }
            },
            phoneClick(sn) {
                let w = 0, h = 0;
                for (const device of this.devices) {
                    if (device.sn === sn) {
                        w = device.w;
                        h = device.h;
                        break;
                    }
                }
                if (w === 0 || h === 0) {
                    w = 1080;
                    h = 1920;
                }
                const url = 'device.html?sn=' + encodeURIComponent(sn) + '&w=' + w + '&h=' + h;
                window.open(url, '_blank');
            }
        }
    });

    // ========== 服务器列表 Vue 组件 ==========
    const serverList = new Vue({
        data: {
            servers: []
        },
        methods: {
            addServer(ip, port) {
                // 检查是否已存在
                const exists = this.servers.some(s => s.ip === ip && s.port === port);
                if (exists) {
                    console.warn('服务器已添加:', ip + ':' + port);
                    return;
                }

                const server = new Server(ip, port);
                this.servers.push(server);
                server.connect();
            },
            updateServerStatus(ip, port, connected) {
                const server = this.servers.find(s => s.ip === ip && s.port === port);
                if (server) {
                    server.connected = connected;
                }
            },
            removeServer(ip, port) {
                const idx = this.servers.findIndex(s => s.ip === ip && s.port === port);
                if (idx !== -1) {
                    this.servers[idx].disconnect();
                    this.servers.splice(idx, 1);
                }
            }
        }
    });

    // ========== 初始化 ==========
    function init() {
        // 自动连接本地服务器
        serverList.addServer(DEFAULT_IP, DEFAULT_PORT);

        // 添加服务器按钮
        $('#btn-addserver').on('click', function() {
            const val = $('#server-input').val().trim();
            if (!val) return;

            let ip = val;
            let port = DEFAULT_PORT;

            if (val.indexOf(':') !== -1) {
                const parts = val.split(':');
                ip = parts[0];
                port = parseInt(parts[1]) || DEFAULT_PORT;
            }

            serverList.addServer(ip, port);
            $('#server-input').val('');
            $('#serverModal').modal('hide');
        });

        // 回车键添加服务器
        $('#server-input').on('keypress', function(event) {
            if (event.keyCode === 13) {
                $('#btn-addserver').click();
            }
        });

        // ADB 重启按钮
        $('#btn-restart-adb').on('click', function() {
            const btn = $(this);
            const icon = btn.find('.fa');
            const originalHtml = btn.html();

            // 禁用按钮，显示加载状态
            btn.prop('disabled', true);
            icon.removeClass('fa-refresh').addClass('fa-spinner fa-spin');
            btn.find('.fa').next().remove(); // 移除旧文本节点
            btn.append(' 重启中...');

            // 获取当前连接的服务器地址
            const server = serverList.servers.length > 0 ? serverList.servers[0] : null;
            if (!server) {
                alert('没有连接的服务器');
                btn.prop('disabled', false);
                btn.html(originalHtml);
                return;
            }

            // 发送 HTTP 请求到 /restart-adb
            $.ajax({
                url: 'http://' + server.ip + ':' + server.port + '/restart-adb',
                type: 'GET',
                timeout: 15000,
                success: function(data) {
                    if (data.status === 'ok') {
                        // 重启成功后刷新设备列表
                        setTimeout(function() {
                            server.net.send('M_DEVICES');
                        }, 2000);
                    }
                    alert(data.message || (data.status === 'ok' ? 'ADB 重启成功' : 'ADB 重启失败'));
                },
                error: function(xhr, status, err) {
                    alert('ADB 重启请求失败: ' + (err || status));
                },
                complete: function() {
                    btn.prop('disabled', false);
                    btn.html(originalHtml);
                }
            });
        });
    }

    // ========== 启动 ==========
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

})();
