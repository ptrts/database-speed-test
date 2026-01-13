Откройте PowerShell, переключитесь в каталог [./terraform](./terraform)

### Установите Yandex Cloud CLI (yc), и настройте профиль yc 

https://yandex.cloud/ru/docs/cli/quickstart

В процессе выберите `[2] Create a new profile`, 
введите имя нового **профиля** `database-speed-test`. 

Потом, выберите `[3] Create a new folder`, 
введите имя нового **каталога** `database-speed-test`. 

### Сохраните ID облака

Получите ID облака
```PowerShell
yc config get cloud-id
```

Сохраните ID облака в [./terraform/01-YC_CLOUD_ID.private.ps1](./terraform/01-YC_CLOUD_ID.private.ps1)
и выполните этот файл в PowerShell, чтобы установилась переменная окружения `YC_CLOUD_ID`.

### Сохраните ID каталога database-speed-test

Сохраните ID каталога в [./terraform/02-YC_FOLDER_ID.private.ps1](./terraform/02-YC_FOLDER_ID.private.ps1)
и выполните этот файл в PowerShell, чтобы установилась переменная окружения `YC_FOLDER_ID`.

### Создайте в каталоге database-speed-test service account для Terraform

```PowerShell
yc iam service-account create --name terraform
```

Скопируйте в буфер обмена идентификатор созданного service account. 

Или, запросите его вот так:

```PowerShell
yc iam service-account get terraform
```

Сохраните полученный ID в [./terraform/03-YC_SA_TERRAFORM.private.ps1](./terraform/03-YC_SA_TERRAFORM.private.ps1)
и выполните этот файл, чтобы установилась переменная окружения `YC_SA_TERRAFORM`. 

### Назначьте сервисному аккаунту terraform роли

TODO

### Добавьте аутентификационные данные в переменные окружения

```PowerShell
$Env:YC_TOKEN=$(yc iam create-token --impersonate-service-account-id $Env:YC_SA_TERRAFORM)
```

Срок действия этого токена YC_TOKEN периодически истекает. 
Токен приходится периодически обновлять, 
выполняя файл [./terraform/04-YC_TOKEN.ps1](./terraform/04-YC_TOKEN.ps1), 
который содержит ту команду выше.

### В каждом сеансы работы в PowerShell, обновляйте переменные окружения

В новом сеансе PowerShell выполняйте следующие файлы для обновления переменных окружения:
- [./terraform/01-YC_CLOUD_ID.private.ps1](./terraform/01-YC_CLOUD_ID.private.ps1)
- [./terraform/02-YC_FOLDER_ID.private.ps1](./terraform/02-YC_FOLDER_ID.private.ps1)
- [./terraform/03-YC_SA_TERRAFORM.private.ps1](./terraform/03-YC_SA_TERRAFORM.private.ps1)
- [./terraform/04-YC_TOKEN.ps1](./terraform/04-YC_TOKEN.ps1)

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

### Далее

```PowerShell
terraform init
```
