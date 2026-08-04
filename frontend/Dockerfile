# 1. AŞAMA: Derleme (Build) Ortamı
FROM node:18-alpine as build
WORKDIR /app

# Önce sadece package.json dosyalarını kopyalayıp bağımlılıkları yüklüyoruz (Docker Cache için)
COPY package.json package-lock.json ./
RUN npm install

# Şimdi tüm kodları kopyalayıp production build alıyoruz
COPY . .
RUN npm run build

# 2. AŞAMA: Sunum (Serve) Ortamı
FROM nginx:alpine

# Nginx'in varsayılan sayfasını siliyoruz
RUN rm -rf /usr/share/nginx/html/*

# 1. Aşamada oluşturduğumuz build klasöründeki dosyaları Nginx'in içine kopyalıyoruz
COPY --from=build /app/build /usr/share/nginx/html

# Kendi Nginx ayar dosyamızı kopyalıyoruz
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]