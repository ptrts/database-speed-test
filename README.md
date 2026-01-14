Далее идет описание как поднимать VM 
из руководства про использование Terraform в Yandex Cloud. 

Создаваемые здесь VM - они не про наш database-speed-test, 
а просто из руководства. 

Развитие этой темы у нас пока на паузе. 

Откройте PowerShell, переключитесь в каталог [./terraform](./terraform)

### Установите Yandex Cloud CLI (yc), и настройте профиль yc 

https://yandex.cloud/ru/docs/cli/quickstart

Нам нужно чтоб в каком-то из доступных вам облаков был каталог `database-speed-test`, 
и чтобы на вашем компьютере был профиль yc тоже с названием `database-speed-test`, 
настроенный для работы с этим облаком и с этим каталогом. 

Поэтому, во время работы `yc init` выберите `[2] Create a new profile`, 
введите имя нового **профиля** `database-speed-test`. 

Потом, выберите `[3] Create a new folder`, 
введите имя нового **каталога** `database-speed-test`. 

### Создайте в каталоге database-speed-test service account для Terraform

```PowerShell
yc iam service-account create --name terraform
```

### Проставьте переменные окружения

Запустите следующий скрипт:
```PowerShell
./env-long.ps1
```

В результате будет создан другой скрипт [./terraform/env.private.ps1](./terraform/env.private.ps1), 
устанавливающий следующие переменные окружения:
- `YC_CLOUD_ID`
- `YC_FOLDER_ID`
- `YC_SA_TERRAFORM_ID`

Данный созданный скрипт будет автоматически выполнен, 
упомянутые переменные окружения будут автоматически проставлены в текущий сеанс PowerShell. 

Также будет сформирован IAM токен для работы от имени созданного нами ранее сервисного аккаунта `terraform`. 
Этот IAM токен будет проставлен в переменную окружения `YC_TOKEN`.

Срок действия этого токена `YC_TOKEN` периодически истекает, 
и его приходится обновлять 
скриптом [./terraform/env-fast.ps1](./terraform/env-fast.ps1).

Также [./terraform/env-fast.ps1](./terraform/env-fast.ps1) нужно выполнять перед работой с terraform в новом сеансе PowerShell, 
где еще не проставлены все нужные переменные окружения. 

### Назначьте сервисному аккаунту terraform роли

```PowerShell
yc resource-manager folder add-access-binding $Env:YC_FOLDER_ID --role editor --subject "serviceAccount:$Env:YC_SA_TERRAFORM"
yc resource-manager folder add-access-binding $Env:YC_FOLDER_ID --role compute.osLogin --subject "serviceAccount:$Env:YC_SA_TERRAFORM"
```

### Устанавливаем Terraform CLI (terraform)

https://developer.hashicorp.com/terraform/tutorials/aws-get-started/install-cli

### Настраиваем Terraform CLI для YC

#### Сохраняем имеющийся terraform.rc, если есть

```PowerShell
mv $env:APPDATA/terraform.rc $env:APPDATA/terraform.rc.old
```

#### Создаем новый terraform.rc

```PowerShell
notepad $env:APPDATA/terraform.rc
```

Нажимаем кнопку, что нужно создать новый файл. 

Сохраняем новый файл terraform.rc со следующим содержимым:
```HCL
provider_installation {
  network_mirror {
    url = "https://terraform-mirror.yandexcloud.net/"
    include = ["registry.terraform.io/*/*"]
  }
  direct {
    exclude = ["registry.terraform.io/*/*"]
  }
}
```

### Инициализация Terraform

```PowerShell
terraform init
```

Удалите файл [./terraform/.terraform.lock.hcl](./terraform/.terraform.lock.hcl). 

Сгенерируйте lock файл снова, следующей командой:
```PowerShell
terraform providers lock -net-mirror=https://terraform-mirror.yandexcloud.net -platform=windows_amd64 -platform=linux_amd64 -platform=darwin_arm64 yandex-cloud/yandex
```

### Генерируем SSH ключи для доступа к VM

ssh-keygen -t ed25519

Будут сформированы вот такая пара ключей:
- [./terraform/id_ed25519](./terraform/id_ed25519)
- [./terraform/id_ed25519.pub](./terraform/id_ed25519.pub)

Эти ключи проигнорированы в [gitignore](.gitignore). 

### Если нужно создавать VM на базе другого образа

У нас используется вот такой образ
```
ID          : fd80von1v2g6rjn7oofk
NAME        : ubuntu-2404-lts-oslogin-v20251117
FAMILY      : ubuntu-2404-lts-oslogin
PRODUCT IDS : f2evival4mdpe3hpjhme
```

Но, можно использовать и другие образы. 

Получите список всех доступных образов. 
```PowerShell
yc compute image list --folder-id standard-images
```

При необходимости, отфильтруйте этот список каким-то похожим образом:
```PowerShell
yc compute image list --folder-id standard-images | Select-String -Pattern " ubuntu-2404-lts-oslogin-v202511"
```

Выберите образ, скопируйте его ID и используйте его в [./terraform/main.tf](./terraform/main.tf)

### Создайте ресурсы

```PowerShell
terraform plan
```

```PowerShell
terraform apply
```

### Проверьте доступ к VM

```PowerShell
ssh -i id_ed25519 myuser@<публичный IP>
```

### Удаление ресурсов

```PowerShell
terraform destroy
```
