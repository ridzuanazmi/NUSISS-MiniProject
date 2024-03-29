import { Component, OnInit } from '@angular/core';
import { FormGroup, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { RegisterResponse } from 'src/app/models/models';
import { RegisterService } from 'src/app/services/register.service';
import { MatchPassword } from 'src/app/validator/match-password.validator';
import { PasswordStrengthValidator } from 'src/app/validator/password.validator';
import { getCookie } from 'typescript-cookie';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {

  registerForm!: FormGroup;
  message!: RegisterResponse;
  submitted = false;
  errorMessage!: string;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private registerSrvc: RegisterService) { }

  ngOnInit(): void {
    this.registerForm = this.createForm();
  }

  onSubmit() {
    this.submitted = true;
    if (this.registerForm.invalid) {
      return;
    }

    console.log("Register - onSubmit(): ", this.registerForm.value);
    this.registerSrvc.register(this.registerForm.value)
      .then(response => {
        console.info('>>> signup(): result: ', response);
        this.message = response;
      })
      .catch((error) => {
        console.error('>>> signup(): error: ', error);
        this.message = error;
        // Get CSRF cookie from Spring
        let xsrf = getCookie('XSRF-TOKEN')!;
        console.info("XSRF-TOKEN = ", xsrf);
        window.sessionStorage.setItem('XSRF-TOKEN', xsrf);
        this.errorMessage = 'Failed to create an account. Please refresh and try again.';
      })

    this.router.navigate(['/login']) // After registration, do login
  }

  private createForm(): FormGroup {
    return this.fb.group({
      fullName: ['', [
        Validators.required,
        Validators.minLength(2)]],
      phoneNumber: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^[89]\d{7,}$/)]],
      email: ['', [
        Validators.required,
        Validators.email]],
      password: ['', [
        Validators.required,
        PasswordStrengthValidator()
      ]],
      confirmPassword: ['',
        Validators.required,],
      role: ['USER',
        Validators.required,],
      hidePassword: ['']
    },
      {
        validator: MatchPassword
      })
  }
}
