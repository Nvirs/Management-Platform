variable "cluster_name" {
  type = string
}

variable "cluster_version" {
  description = "Kubernetes version for the EKS control plane"
  type        = string
  default     = "1.31"
}

variable "vpc_id" {
  type = string
}

variable "private_subnet_ids" {
  description = "Worker nodes are placed here"
  type        = list(string)
}

variable "public_subnet_ids" {
  description = "Given to the control plane too, so a public-facing ALB/Ingress can be provisioned later"
  type        = list(string)
}

variable "endpoint_public_access" {
  description = "Allow kubectl access from the public internet (simplest for a solo demo cluster)"
  type        = bool
  default     = true
}

variable "endpoint_private_access" {
  type    = bool
  default = true
}

variable "node_instance_types" {
  type    = list(string)
  default = ["t3.small"]
}

variable "node_capacity_type" {
  description = "ON_DEMAND or SPOT -- SPOT is meaningfully cheaper and fine for a throwaway demo cluster"
  type        = string
  default     = "SPOT"
}

variable "node_disk_size" {
  type    = number
  default = 20
}

variable "node_desired_size" {
  type    = number
  default = 2
}

variable "node_min_size" {
  type    = number
  default = 1
}

variable "node_max_size" {
  type    = number
  default = 3
}

variable "admin_principal_arns" {
  description = "Extra IAM principal ARNs (besides whoever runs `terraform apply`, who gets admin automatically) to grant cluster-admin access"
  type        = list(string)
  default     = []
}

variable "tags" {
  type    = map(string)
  default = {}
}
