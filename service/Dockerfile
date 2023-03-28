# 基础镜像
FROM java:8
# 作者
MAINTAINER cj
# 添加jar到镜像并命名为user.jar
ADD service-0.0.1-SNAPSHOT.jar im-service.jar
# 镜像启动后暴露的端口
EXPOSE 8081
# jar运行命令，参数使用逗号隔开
ENTRYPOINT ["java","-jar","im-service.jar"]
